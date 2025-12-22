package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;
    private final WalletService walletService;
    private final RewardService rewardService;
    private final EventRepository eventRepo;
    private final UserRepository userRepo;
    private final EventRegistrationRepository regRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final QRTokenRepository qrTokenRepo;
    private final JwtEventTokenService jwtEventTokenService;
    private final EmailService emailService;
    private static final int QR_EXP_SECONDS = 120;
    private final EventRegistrationRepository registrationRepo;
    // =========================================================
    // 1️⃣ Leader tạo QR token
    // =========================================================
    @Override
    public Map<String, Object> getQrTokenForEvent(Long eventId, String phaseStr) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        QRPhase phase = QRPhase.valueOf(phaseStr.toUpperCase());
        String cacheKey = "event:qr:" + eventId + ":" + phase.name();

        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null)
            return Map.of("token", cached, "phase", phase.name(), "expiresIn", QR_EXP_SECONDS);

        String tokenValue = jwtEventTokenService.generateEventToken(eventId, phase.name());
        qrTokenRepo.save(QRToken.builder()
                .event(event)
                .tokenValue(tokenValue)
                .phase(phase)
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusSeconds(QR_EXP_SECONDS))
                .build());

        redis.opsForValue().set(cacheKey, tokenValue, Duration.ofSeconds(QR_EXP_SECONDS));
        return Map.of("token", tokenValue, "phase", phase.name(), "expiresIn", QR_EXP_SECONDS);
    }

    // =========================================================
    // 2️⃣ Member scan QR
    // =========================================================
    @Override
    @Transactional
    public void scanEventPhase(String tokenValue, String email) {
        QRToken token = qrTokenRepo.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "QR token invalid"));
        validateTokenWindow(token);

        Event event = token.getEvent();
        QRPhase phase = token.getPhase();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        processAttendancePhase(user, event, phase);
    }

    // =========================================================
    // 3️⃣ Staff xác nhận thủ công
    // =========================================================
    @Override
    @Transactional
    public void verifyAndSaveAttendance(User user, Event event, String level) {
        QRPhase phase = QRPhase.valueOf(level.toUpperCase());
        processAttendancePhase(user, event, phase);
    }

    // =========================================================
    // 🔹 Core xử lý chung cho START / MID / END
    // =========================================================
    private void processAttendancePhase(User user, Event event, QRPhase phase) {

        // ❌ PUBLIC event không có multi-phase attendance
        if (event.getType() == EventTypeEnum.PUBLIC) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PUBLIC event does not support multi-phase attendance"
            );
        }

        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(
                event.getEventId(), user.getUserId()
        ).orElseThrow(() ->
                new ApiException(HttpStatus.FORBIDDEN, "You have not registered for this event")
        );

        AttendanceRecord record = attendanceRepo
                .findByUser_UserIdAndEvent_EventId(user.getUserId(), event.getEventId())
                .orElseGet(() -> AttendanceRecord.builder()
                        .user(user)
                        .event(event)
                        .build()
                );

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        switch (phase) {
            case START -> {
                if (record.getStartCheckInTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked in (START)");

                record.setStartCheckInTime(now);
                reg.setStatus(RegistrationStatusEnum.CHECKED_IN);
                reg.setCheckinAt(now);

                Integer current = Optional
                        .ofNullable(event.getCurrentCheckInCount())
                        .orElse(0);
                event.setCurrentCheckInCount(current + 1);
                eventRepo.save(event);
            }

            case MID -> {
                if (record.getMidCheckTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked in (MID)");
                if (record.getStartCheckInTime() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Must START before MID");

                record.setMidCheckTime(now);
                reg.setCheckMidAt(now);
            }

            case END -> {
                if (record.getEndCheckOutTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked out (END)");
                if (record.getStartCheckInTime() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Must START before END");

                record.setEndCheckOutTime(now);
                reg.setCheckoutAt(now);

                updateAttendanceLevel(record, reg);
            }
        }

        attendanceRepo.save(record);
        reg.setUpdatedAt(now);
        regRepo.save(reg);
    }





    // =========================================================
// 🔹 Logic tính điểm thưởng (cập nhật chuẩn với multiplier mới)
// =========================================================
    private void applyReward(User user, Event event) {
        Club club = event.getHostClub();
        long commitPoints = event.getCommitPointCost() != null ? event.getCommitPointCost() : 0L;

        // 🔹 1️⃣ Lấy attendance record của user trong event này
        AttendanceRecord record = attendanceRepo.findByUser_UserIdAndEvent_EventId(
                user.getUserId(),
                event.getEventId()
        ).orElse(null);

        if (record == null || record.getAttendanceLevel() == null) {
            log.warn("⚠️ No attendance record found for user {} in event '{}'", user.getEmail(), event.getName());
            return; // Không cộng điểm
        }

        AttendanceLevelEnum level = record.getAttendanceLevel();
        long rewardPoints = 0L;

        // 🔹 2️⃣ Tính điểm dựa theo level
        switch (level) {
            case FULL -> rewardPoints = commitPoints * 2; // Hoàn tất đủ 3 phase
            case HALF -> rewardPoints = commitPoints;     // Hoàn lại commit (x1)
            case SUSPICIOUS, NONE -> rewardPoints = 0;    // Không được thưởng
        }

        // 🔹 3️⃣ Nếu không có điểm thì bỏ qua
        if (rewardPoints <= 0) {
            log.info("User {} did not qualify for rewards (level = {}).", user.getEmail(), level);
            return;
        }

        // 🔹 4️⃣ Cộng điểm vào ví user
        Wallet wallet = walletService.getOrCreateUserWallet(user);
        walletService.increase(wallet, rewardPoints);

        // 🔹 5️⃣ Ghi log giao dịch
        walletService.logClubToMemberReward(
                wallet,
                rewardPoints,
                String.format(
                        "Reward from '%s' [%s participation] (Commit %d × Multiplier %.1fx = %d points)",
                        event.getName(),
                        level.name(),
                        commitPoints,
                        (level == AttendanceLevelEnum.FULL ? 2.0 : 1.0),
                        rewardPoints
                )
        );

        // 🔹 6️⃣ Gửi email thông báo
        rewardService.sendCheckInRewardEmail(
                user.getUserId(),
                event.getName(),
                rewardPoints,
                wallet.getBalancePoints()
        );

        log.info("Reward applied: {} got {} points for event '{}' [{}]",
                user.getEmail(), rewardPoints, event.getName(), level.name());
    }



    // =========================================================
    // 4️⃣ Handlers phụ (START / MID / END)
    // =========================================================
    @Override @Transactional public void handleStartCheckin(User u, Event e) { processAttendancePhase(u, e, QRPhase.START); }
    @Override @Transactional public void handleMidCheckin(User u, Event e) { processAttendancePhase(u, e, QRPhase.MID); }
    @Override @Transactional public void handleEndCheckout(User u, Event e) { processAttendancePhase(u, e, QRPhase.END); }

    // =========================================================
    // 5️⃣ Event stats + Fraud list
    // =========================================================
    @Override
    public EventStatsResponse getEventStats(Long eventId) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // ✅ PUBLIC event
        if (event.getType() == EventTypeEnum.PUBLIC) {
            long checkedIn =
                    attendanceRepo.countByEvent_EventIdAndStartCheckInTimeNotNull(eventId);



            return EventStatsResponse.builder()
                    .eventId(event.getEventId())
                    .eventName(event.getName())
                    .totalRegistered(0)
                    .checkinCount(checkedIn)
                    .midCount(0)
                    .checkoutCount(0)
                    .noneCount(0)
                    .halfCount(0)
                    .fullCount(0)
                    .suspiciousCount(0)
                    .participationRate(0)
                    .midComplianceRate(0)
                    .fraudRate(0)
                    .build();
        }

        // ===== PRIVATE / SPECIAL giữ nguyên =====
        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);
        int total = regs.size();

        int checkin = 0, mid = 0, checkout = 0;
        int none = 0, half = 0, full = 0, suspicious = 0;

        for (EventRegistration r : regs) {
            if (r.getCheckinAt() != null) checkin++;
            if (r.getCheckMidAt() != null) mid++;
            if (r.getCheckoutAt() != null) checkout++;

            AttendanceLevelEnum lv = r.getAttendanceLevel();
            if (lv == null) none++;
            else switch (lv) {
                case FULL -> full++;
                case HALF -> half++;
                case SUSPICIOUS -> suspicious++;
                default -> none++;
            }
        }

        double participationRate = total == 0 ? 0 : ((half + full) * 1.0 / total);
        double midComplianceRate = checkin == 0 ? 0 : (mid * 1.0 / checkin);
        double fraudRate = total == 0 ? 0 : (suspicious * 1.0 / total);

        return EventStatsResponse.builder()
                .eventId(event.getEventId())
                .eventName(event.getName())
                .totalRegistered(total)
                .checkinCount(checkin)
                .midCount(mid)
                .checkoutCount(checkout)
                .noneCount(none)
                .halfCount(half)
                .fullCount(full)
                .suspiciousCount(suspicious)
                .participationRate(round2(participationRate))
                .midComplianceRate(round2(midComplianceRate))
                .fraudRate(round2(fraudRate))
                .build();
    }


    @Override
    public List<FraudCaseResponse> getFraudCases(Long eventId) {
        eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

        List<FraudCaseResponse> out = new ArrayList<>();
        for (EventRegistration r : regs) {
            if (Boolean.TRUE.equals(r.isSuspicious())) {
                out.add(FraudCaseResponse.builder()
                        .memberName(r.getUser().getFullName())
                        .memberEmail(r.getUser().getEmail())
                        .checkinAt(r.getCheckinAt())
                        .checkMidAt(r.getCheckMidAt())
                        .checkoutAt(r.getCheckoutAt())
                        .fraudReason(r.getFraudReason())
                        .build());
            }
        }
        return out;
    }

    // =========================================================
    // ⚙️ Helpers
    // =========================================================
    private void validateTokenWindow(QRToken token) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        if (token.getValidFrom() != null && now.isBefore(token.getValidFrom()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR token not yet active");
        if (token.getValidTo() != null && now.isAfter(token.getValidTo()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR token expired");
    }

    private void updateAttendanceLevel(AttendanceRecord record, EventRegistration reg) {
        boolean hasStart = record.getStartCheckInTime() != null;
        boolean hasMid = record.getMidCheckTime() != null;
        boolean hasEnd = record.getEndCheckOutTime() != null;

        if (hasStart && hasMid && hasEnd) {
            record.setAttendanceLevel(AttendanceLevelEnum.FULL);
            reg.setAttendanceLevel(AttendanceLevelEnum.FULL);
            reg.setSuspicious(false);
        } else if (hasStart && hasMid) {
            record.setAttendanceLevel(AttendanceLevelEnum.HALF);
            reg.setAttendanceLevel(AttendanceLevelEnum.HALF);
            reg.setSuspicious(false);
        } else if (hasStart && hasEnd && !hasMid) {
            record.setAttendanceLevel(AttendanceLevelEnum.SUSPICIOUS);
            reg.setAttendanceLevel(AttendanceLevelEnum.SUSPICIOUS);
            reg.setSuspicious(true);
            reg.setFraudReason("Checked IN and OUT without MID");
        } else {
            record.setAttendanceLevel(AttendanceLevelEnum.NONE);
            reg.setAttendanceLevel(AttendanceLevelEnum.NONE);
            reg.setSuspicious(false);
        }
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    @Override @Transactional
    public void checkInWithToken(String token, String email) {
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "Deprecated check-in method");
    }

    @Override @Transactional
    public String verifyAttendance(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration not found"));
        reg.setAttendanceLevel(AttendanceLevelEnum.FULL);
        reg.setUpdatedAt(LocalDateTime.now());
        regRepo.save(reg);
        return "Verified full attendance (100%) for user ID " + userId;
    }

    @Override
    @Transactional
    public void handlePublicCheckin(User user, Event event) {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        // =====================================================
        // 1️⃣ FIND OR CREATE ATTENDANCE RECORD
        // =====================================================
        AttendanceRecord record = attendanceRepo
                .findByUser_UserIdAndEvent_EventId(user.getUserId(), event.getEventId())
                .orElse(null);

        if (record != null && record.getStartCheckInTime() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You have already checked in.");
        }

        if (record == null) {
            record = AttendanceRecord.builder()
                    .user(user)
                    .event(event)
                    .attendanceLevel(AttendanceLevelEnum.NONE)
                    .build();
        } else if (record.getAttendanceLevel() == null) {
            record.setAttendanceLevel(AttendanceLevelEnum.NONE);
        }

        record.setStartCheckInTime(now);
        attendanceRepo.save(record);

        // =====================================================
        // 2️⃣ CỘNG ĐIỂM THƯỞNG NGAY (PUBLIC)
        // =====================================================
        Long reward = event.getRewardPerParticipant();

        if (reward != null && reward > 0) {

            Wallet eventWallet = event.getWallet();
            if (eventWallet == null || eventWallet.getStatus() == WalletStatusEnum.CLOSED) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Event wallet is not available for reward distribution."
                );
            }

            if (eventWallet.getBalancePoints() < reward) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Event wallet does not have enough points."
                );
            }

            Wallet userWallet = walletService.getOrCreateUserWallet(user);

            walletService.transferPointsWithType(
                    eventWallet,
                    userWallet,
                    reward,
                    "Public event check-in reward: " + event.getName(),
                    WalletTransactionTypeEnum.BONUS_REWARD
            );
        }

        // =====================================================
        // 3️⃣ GỬI EMAIL (KHÔNG ĐƯỢC ROLLBACK ĐIỂM)
        // =====================================================
        try {
            emailService.sendPublicEventCheckinEmail(
                    user.getEmail(),
                    user.getFullName(),
                    event.getName(),
                    now.toLocalTime(),
                    event.getLocation() != null
                            ? event.getLocation().getName()
                            : "Unknown",
                    reward
            );
        } catch (Exception e) {
            log.error("❌ Failed to send check-in email, reward already granted", e);
        }

        log.info(
                "✅ PUBLIC CHECK-IN SUCCESS user={} event={} reward={}",
                user.getEmail(),
                event.getName(),
                reward
        );
    }





    @Override
    public List<EventAttendeeResponse> getEventAttendees(Long eventId) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<AttendanceRecord> records =
                attendanceRepo.findByEvent_EventIdAndStartCheckInTimeNotNull(eventId);

        return records.stream()
                .map(ar -> {
                    AttendanceLevelEnum level =
                            event.getType() == EventTypeEnum.PUBLIC
                                    ? AttendanceLevelEnum.NONE
                                    : ar.getAttendanceLevel();

                    return new EventAttendeeResponse(
                            ar.getUser().getUserId(),
                            ar.getUser().getFullName(),
                            ar.getUser().getEmail(),
                            level,
                            ar.getStartCheckInTime(),
                            ar.getMidCheckTime(),
                            ar.getEndCheckOutTime()
                    );
                })
                .toList();
    }



    @Override
    @Transactional
    public List<MyCheckedInEventResponse> getMyCheckedInEvents(Long userId) {

        List<AttendanceRecord> records =
                attendanceRepo.findMyCheckedInEvents(userId);

        return records.stream()
                .map(ar -> MyCheckedInEventResponse.builder()
                        .eventId(ar.getEvent().getEventId())
                        .eventName(ar.getEvent().getName())
                        .startChecked(ar.getStartCheckInTime() != null)
                        .midChecked(ar.getMidCheckTime() != null)
                        .endChecked(ar.getEndCheckOutTime() != null)
                        .attendanceLevel(ar.getAttendanceLevel())
                        .build()
                )
                .toList();
    }


    @Override
    public List<EventRegisteredUserResponse> getRegisteredUsers(Long eventId) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

        return regs.stream()
                .map(r -> new EventRegisteredUserResponse(
                        r.getUser().getUserId(),
                        r.getUser().getFullName(),
                        r.getUser().getEmail(),
                        r.getStatus(),
                        r.getRegisteredAt(),
                        r.getCommittedPoints()
                ))
                .toList();
    }
    @Override
    @Transactional
    public Map<String, Object> publicQrCheckIn(User user, String tokenValue) {

        // 1️⃣ Verify QR token
        QRToken token = qrTokenRepo.findByTokenValue(tokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid QR token"));

        validateTokenWindow(token);

        Event event = token.getEvent();

        // 2️⃣ Chỉ cho PUBLIC event
        if (event.getType() != EventTypeEnum.PUBLIC) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not a PUBLIC event");
        }

        // 3️⃣ Kiểm tra event đang diễn ra
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        boolean active = event.getDays().stream().anyMatch(day -> {
            LocalDateTime start = LocalDateTime.of(day.getDate(), day.getStartTime());
            LocalDateTime end   = LocalDateTime.of(day.getDate(), day.getEndTime());
            return !now.isBefore(start) && !now.isAfter(end);
        });

        if (!active) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not active now");
        }

        // 4️⃣ AttendanceRecord
        AttendanceRecord record = attendanceRepo
                .findByUser_UserIdAndEvent_EventId(user.getUserId(), event.getEventId())
                .orElse(null);

        if (record != null && record.getStartCheckInTime() != null) {
            return Map.of(
                    "eventId", event.getEventId(),
                    "checkedIn", false,
                    "message", "Already checked in"
            );
        }

        if (record == null) {
            record = AttendanceRecord.builder()
                    .user(user)
                    .event(event)
                    .attendanceLevel(AttendanceLevelEnum.NONE)
                    .build();
        } else if (record.getAttendanceLevel() == null) {
            record.setAttendanceLevel(AttendanceLevelEnum.NONE);
        }

        record.setStartCheckInTime(now);
        attendanceRepo.save(record);

        // 5️⃣ Update event counter
//        int current = Optional.ofNullable(event.getCurrentCheckInCount()).orElse(0);
//        event.setCurrentCheckInCount(current + 1);
//        eventRepo.save(event);

        // ===============================
        // 🔥 6️⃣ PHÁT ĐIỂM NGAY KHI CHECK-IN
        // ===============================
        Long reward = event.getRewardPerParticipant();

        if (reward != null && reward > 0) {

            Wallet eventWallet = event.getWallet();
            if (eventWallet == null || eventWallet.getStatus() == WalletStatusEnum.CLOSED) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Event wallet invalid");
            }

            if (eventWallet.getBalancePoints() < reward) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Event wallet insufficient");
            }

            Wallet userWallet = walletService.getOrCreateUserWallet(user);

            walletService.transferPointsWithType(
                    eventWallet,
                    userWallet,
                    reward,
                    "Public event reward (QR): " + event.getName(),
                    WalletTransactionTypeEnum.BONUS_REWARD
            );
        }

        // ===============================
        // 7️⃣ EMAIL – KHÔNG ĐƯỢC ROLLBACK
        // ===============================
        try {
            emailService.sendPublicEventCheckinEmail(
                    user.getEmail(),
                    user.getFullName(),
                    event.getName(),
                    now.toLocalTime(),
                    event.getLocation() != null
                            ? event.getLocation().getName()
                            : "Unknown",
                    reward
            );
        } catch (Exception e) {
            log.error("Email failed but reward already granted", e);
        }

        return Map.of(
                "eventId", event.getEventId(),
                "checkedIn", true,
                "reward", reward
        );
    }

    @Override
    public boolean checkPublicEventCheckedIn(User user, String checkInCode) {

        // 1️⃣ Xác định event từ checkInCode
        Event event = eventRepo.findByCheckInCode(checkInCode)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Invalid check-in code"
                ));

        // 2️⃣ Chỉ cho PUBLIC event
        if (event.getType() != EventTypeEnum.PUBLIC) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "This API is only for PUBLIC events"
            );
        }

        // 3️⃣ Kiểm tra user đã check-in chưa
        return attendanceRepo.existsByUser_UserIdAndEvent_EventId(
                user.getUserId(),
                event.getEventId()
        );
    }
    @Override
    public Map<String, Object> getPublicCheckInStatus(User user, String checkInCode) {

        Event event = eventRepo.findByCheckInCode(checkInCode)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code")
                );

        if (event.getType() != EventTypeEnum.PUBLIC) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "This event is not a PUBLIC event"
            );
        }

        AttendanceRecord record =
                attendanceRepo.findByEvent_EventIdAndUser_UserId(
                        event.getEventId(),
                        user.getUserId()
                ).orElse(null);

        return Map.of(
                "eventId", event.getEventId(),
                "eventType", EventTypeEnum.PUBLIC,
                "checkedIn", record != null && record.getStartCheckInTime() != null,
                "checkedAt", record != null ? record.getStartCheckInTime() : null
        );
    }
    @Override
    public MyEventAttendanceStatusResponse getMyAttendanceStatusByCheckInCode(
            User user,
            String checkInCode
    ) {

        Event event = eventRepo.findByCheckInCode(checkInCode)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code")
                );

        // ❌ Nếu không muốn áp dụng cho PUBLIC
        if (event.getType() == EventTypeEnum.PUBLIC) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Use public attendance status API for PUBLIC event"
            );
        }

        Long eventId = event.getEventId();
        Long userId = user.getUserId();

        // 1️⃣ Kiểm tra đăng ký
        EventRegistration registration =
                registrationRepo.findByEvent_EventIdAndUser_UserId(
                        eventId,
                        userId
                ).orElse(null);

        if (registration == null) {
            return MyEventAttendanceStatusResponse.builder()
                    .eventId(eventId)
                    .eventType(event.getType())
                    .registered(false)
                    .checkedInStart(false)
                    .checkedInMid(false)
                    .checkedInEnd(false)
                    .fullyCheckedIn(false)
                    .build();
        }

        // 2️⃣ Attendance record
        AttendanceRecord record =
                attendanceRepo.findByEvent_EventIdAndUser_UserId(
                        eventId,
                        userId
                ).orElse(null);

        boolean start = record != null && record.getStartCheckInTime() != null;
        boolean mid   = record != null && record.getMidCheckTime() != null;
        boolean end   = record != null && record.getEndCheckOutTime() != null;

        return MyEventAttendanceStatusResponse.builder()
                .eventId(eventId)
                .eventType(event.getType())
                .registered(true)
                .checkedInStart(start)
                .checkedInMid(mid)
                .checkedInEnd(end)
                .fullyCheckedIn(start && mid && end)
                .build();
    }

    @Override
    public MyEventAttendanceStatusResponse getMyAttendanceStatus(User user, Long eventId) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "Event not found")
                );

        if (event.getType() == EventTypeEnum.PUBLIC) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Use public attendance status API for PUBLIC event"
            );
        }

        // 1️⃣ Kiểm tra đăng ký
        EventRegistration registration =
                registrationRepo.findByEvent_EventIdAndUser_UserId(
                        eventId,
                        user.getUserId()
                ).orElse(null);

        if (registration == null) {
            return MyEventAttendanceStatusResponse.builder()
                    .eventId(eventId)
                    .eventType(event.getType())
                    .registered(false)
                    .checkedInStart(false)
                    .checkedInMid(false)
                    .checkedInEnd(false)
                    .fullyCheckedIn(false)
                    .build();
        }

        // 2️⃣ Lấy attendance record
        AttendanceRecord record =
                attendanceRepo.findByEvent_EventIdAndUser_UserId(
                        eventId,
                        user.getUserId()
                ).orElse(null);

        boolean start = record != null && record.getStartCheckInTime() != null;
        boolean mid   = record != null && record.getMidCheckTime() != null;
        boolean end   = record != null && record.getEndCheckOutTime() != null;

        return MyEventAttendanceStatusResponse.builder()
                .eventId(eventId)
                .eventType(event.getType())
                .registered(true)
                .checkedInStart(start)
                .checkedInMid(mid)
                .checkedInEnd(end)
                .fullyCheckedIn(start && mid && end)
                .build();
    }

}
