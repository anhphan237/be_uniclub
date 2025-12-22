package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCheckinRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventRegisterRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPointsServiceImpl implements EventPointsService {
    private final WalletTransactionRepository walletTransactionRepo;
    private final EventLogService eventLogService;
    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final WalletService walletService;
    private final MembershipRepository membershipRepo;
    private final JwtEventTokenService jwtEventTokenService;
    private final AttendanceService attendanceService;
    private final EmailService emailService;
    private final RewardService rewardService;
    private final AttendanceRecordRepository attendanceRepo;
    // =========================================================
    // 🔹 REGISTER
    // =========================================================
    @Override
    @Transactional
    public String register(CustomUserDetails principal, EventRegisterRequest req) {

        User user = userRepo.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // ❌ PUBLIC không cần đăng ký
        if (event.getType() == EventTypeEnum.PUBLIC) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Public events do not require registration.");
        }

        // ❌ Event không ở trạng thái mở đăng ký
        if (!(event.getStatus() == EventStatusEnum.APPROVED
                || event.getStatus() == EventStatusEnum.ONGOING)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for registration.");
        }

        // ⭐ Lấy earliestDay và latestDay (multi-day logic)
        EventDay earliestDay = event.getDays().stream().min(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Event days missing"));

        EventDay latestDay = event.getDays().stream()
                .max(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getEndTime))
                .orElseThrow();

        LocalDate today = LocalDate.now();

        // ❌ Event đã kết thúc (multi-day check)
        if (latestDay.getDate().isBefore(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The event has already ended.");
        }

        // ❌ Không được đăng ký khi event đã bắt đầu
        LocalDateTime eventStart = LocalDateTime.of(
                earliestDay.getDate(),
                earliestDay.getStartTime()
        );

        if (LocalDateTime.now().isAfter(eventStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This event has already started.");
        }


        // ❌ Deadline quá hạn
        if (event.getRegistrationDeadline() != null
                && LocalDateTime.now().isAfter(eventStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Registration deadline has passed.");
        }

        // ❌ Không cho đăng ký trùng
        if (regRepo.existsByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already registered for this event.");
        }
// ❌ Event đã đủ số lượng đăng ký
        long confirmedCount = regRepo.countByEvent_EventIdAndStatus(
                event.getEventId(),
                RegistrationStatusEnum.CONFIRMED
        );

        if (event.getMaxCheckInCount() != null
                && event.getMaxCheckInCount() > 0
                && confirmedCount >= event.getMaxCheckInCount()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "This event has reached the maximum number of participants."
            );
        }

        // 🔐 PRIVATE = chỉ member CLB chủ trì mới được đăng ký
        if (event.getType() == EventTypeEnum.PRIVATE) {
            boolean isHostMember = membershipRepo
                    .existsByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId());

            if (!isHostMember) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Private event: only members of the host club can register.");
            }
        }

        // 🤝 SPECIAL = member host hoặc member cohost
        if (event.getType() == EventTypeEnum.SPECIAL) {

            boolean isMemberHost = membershipRepo
                    .existsByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId());

            boolean isMemberCoHost = event.getCoHostRelations().stream()
                    .anyMatch(rel ->
                            membershipRepo.existsByUser_UserIdAndClub_ClubId(
                                    user.getUserId(),
                                    rel.getClub().getClubId()
                            )
                    );

            if (!isMemberHost && !isMemberCoHost) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "You must be a host or co-host club member to join this event.");
            }
        }

        // 🪙 Trừ commit point
        Wallet userWallet = walletService.getOrCreateUserWallet(user);
        Wallet eventWallet = ensureEventWallet(event);

        long commitPoint = Optional.ofNullable(event.getCommitPointCost()).orElse(0);

        if (userWallet.getBalancePoints() < commitPoint) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points to register.");
        }

        walletService.transferPointsWithType(
                userWallet, eventWallet, commitPoint,
                "Register event " + event.getName(),
                WalletTransactionTypeEnum.COMMIT_LOCK
        );

        // 💾 Lưu registration
        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatusEnum.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .committedPoints((int) commitPoint)
                .attendanceLevel(AttendanceLevelEnum.NONE)
                .build();

        regRepo.save(registration);

        // 📧 Email confirm — MULTI-DAY VERSION
        emailService.sendEventRegistrationEmail(
                user.getEmail(),
                user.getFullName(),
                event,
                commitPoint
        );

        return "Registered successfully. " + commitPoint + " commitment points locked.";
    }

    // =========================================================
    // 🔹 CHECK-IN
    // =========================================================
    @Override
    @Transactional
    public String checkin(CustomUserDetails principal, EventCheckinRequest req) {

        // ===================== 1️⃣ Validate token =====================
        String token = req.getEventJwtToken();
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing check-in QR token.");
        }

        Long eventId;
        try {
            eventId = jwtEventTokenService.parseEventId(token);
        } catch (Exception e) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Invalid or expired check-in QR code."
            );
        }

        User user = principal.getUser();

        // ===================== 2️⃣ Load event WITH LOCK =====================
        Event event = eventRepo.findByIdForPublicCheckin(eventId);
        if (event == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        }

        // ===================== 3️⃣ PUBLIC event =====================
        if (event.getType() == EventTypeEnum.PUBLIC) {

            // ❌ Status không hợp lệ
            if (!(event.getStatus() == EventStatusEnum.APPROVED
                    || event.getStatus() == EventStatusEnum.ONGOING)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Event is not open for check-in."
                );
            }

            // ❌ ĐÃ ĐỦ SLOT → CHẶN LUÔN
//            int current = Optional.ofNullable(event.getCurrentCheckInCount()).orElse(0);
//            int max = Optional.ofNullable(event.getMaxCheckInCount()).orElse(0);
//
//            if (max > 0 && current >= max) {
//                throw new ApiException(
//                        HttpStatus.BAD_REQUEST,
//                        "This event has reached the maximum number of check-ins."
//                );
//            }
            int max = Optional.ofNullable(event.getMaxCheckInCount()).orElse(0);

            if (max > 0) {
                long checkedIn =
                        attendanceRepo.countPublicCheckedIn(event.getEventId());

                if (checkedIn >= max) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "This event has reached the maximum number of check-ins."
                    );
                }
            }

            // ✅ Handle PUBLIC check-in (check trùng + phát điểm)
            attendanceService.handlePublicCheckin(user, event);

            // 📝 Log
            eventLogService.logAction(
                    user.getUserId(),
                    user.getFullName(),
                    event.getEventId(),
                    event.getName(),
                    UserActionEnum.CHECKIN_EVENT,
                    "User performed PUBLIC check-in"
            );

            return "Checked in successfully for PUBLIC event.";
        }

        // ===================== 4️⃣ NON-PUBLIC: must be registered =====================
        EventRegistration reg = regRepo
                .findByEvent_EventIdAndUser_UserId(eventId, user.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN,
                        "You must register for this event before check-in."
                ));

        // ===================== 5️⃣ Event status =====================
        if (!(event.getStatus() == EventStatusEnum.APPROVED
                || event.getStatus() == EventStatusEnum.ONGOING)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Event is not open for check-in."
            );
        }

        // ===================== 6️⃣ Already checked in =====================
        if (reg.getAttendanceLevel() != null
                && reg.getAttendanceLevel() != AttendanceLevelEnum.NONE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "You have already checked in."
            );
        }

        // ===================== 7️⃣ Handle check-in phase =====================
        switch (req.getLevel().toUpperCase()) {
            case "START" -> attendanceService.handleStartCheckin(user, event);
            case "MID"   -> attendanceService.handleMidCheckin(user, event);
            case "END"   -> attendanceService.handleEndCheckout(user, event);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid check-in phase: " + req.getLevel()
            );
        }

        // ===================== 8️⃣ Log =====================
        eventLogService.logAction(
                user.getUserId(),
                user.getFullName(),
                event.getEventId(),
                event.getName(),
                req.getLevel().equalsIgnoreCase("END")
                        ? UserActionEnum.CHECKOUT_EVENT
                        : UserActionEnum.CHECKIN_EVENT,
                "User performed " + req.getLevel().toUpperCase() + " check-in"
        );

        return "Check-in " + req.getLevel().toUpperCase() + " successful.";
    }



    // =========================================================
    // 🔹 CANCEL REGISTRATION
    // =========================================================
    @Override
    @Transactional
    public String cancelRegistration(CustomUserDetails principal, Long eventId) {

        User user = principal.getUser();

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not registered for this event"));

        // ❌ Event canceled → user không cần hủy
        if (event.getStatus() == EventStatusEnum.CANCELLED) {
            return "Event was cancelled by the host. Your points were refunded automatically if applicable.";
        }

        // ❌ Already canceled
        if (reg.getStatus() == RegistrationStatusEnum.CANCELED) {
            return "Registration already canceled.";
        }

        // ============================================================
        // 🔥 MULTI-DAY: LẤY NGÀY BẮT ĐẦU
        // ============================================================
        EventDay earliestDay = event.getDays().stream()
                .sorted(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Event has no days"));

        LocalDate eventStartDate = earliestDay.getDate();
        LocalTime eventStartTime = earliestDay.getStartTime();
        LocalDateTime eventStartDateTime = LocalDateTime.of(eventStartDate, eventStartTime);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));


        // ❌ Không được hủy nếu event đã bắt đầu
        if (now.isAfter(eventStartDateTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel registration after event has started.");
        }

        // ❌ Không hủy được nếu đã check-in
        if (reg.getAttendanceLevel() != null && reg.getAttendanceLevel() != AttendanceLevelEnum.NONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You cannot cancel because you have already checked in.");
        }

        // ============================================================
        // 🔥 KHÔNG REFUND COMMIT POINT CHO REGISTRATION CANCELED
        // ============================================================
        long committed = Optional.ofNullable(reg.getCommittedPoints())
                .map(Integer::longValue)
                .orElse(0L);

        // 🔄 Update status
        reg.setStatus(RegistrationStatusEnum.CANCELED);
        reg.setCancelledAt(LocalDateTime.now());
        regRepo.save(reg);

        // 📧 Email NO-REFUND (multi-day version)
        emailService.sendEventCancellationEmail(
                user.getEmail(),
                user.getFullName(),
                event,          // email tự lấy range ngày
                0               // refund = 0
        );

        return "Registration cancelled. Commitment points will not be refunded.";
    }


    // =========================================================
    // 🔹 END EVENT (final fixed version)
    // =========================================================
    @Override
    @Transactional
    public String endEvent(CustomUserDetails principal, EventEndRequest req) {

        Event event = eventRepo.findByIdWithCoHostRelations(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getHostClub() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Event must have a host club before ending."
            );
        }

        Wallet eventWallet = ensureEventWallet(event);

        // =====================================================
        // 🔥 PUBLIC EVENT: KHÔNG SETTLE THEO COMMIT
        // =====================================================
        if (event.getType() == EventTypeEnum.PUBLIC) {

            // ❌ PUBLIC không phát thưởng ở đây
            // ❌ Không xử lý commit / attendance level

            // 👉 Chỉ cần:
            // - đóng ví
            // - refund leftover
            // - set COMPLETED

            rewardService.autoSettleEvent(event);

            eventWallet.setStatus(WalletStatusEnum.CLOSED);
            walletRepo.save(eventWallet);

            event.setStatus(EventStatusEnum.COMPLETED);
            event.setCompletedAt(LocalDateTime.now());
            eventRepo.save(event);

            return "Public event completed. Rewards were distributed during check-in.";
        }

        // =====================================================
        // 🔒 PRIVATE / SPECIAL EVENT (GIỮ LOGIC CŨ)
        // =====================================================
        List<EventRegistration> regs =
                regRepo.findByEvent_EventId(event.getEventId());

        long totalReward = 0L;

        for (EventRegistration reg : regs) {

            AttendanceLevelEnum level =
                    Optional.ofNullable(reg.getAttendanceLevel())
                            .orElse(AttendanceLevelEnum.NONE);

            long commit =
                    Optional.ofNullable(reg.getCommittedPoints())
                            .map(Integer::longValue)
                            .orElse(0L);

            // ❌ Không commit → NO_SHOW
            if (commit <= 0) {
                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
                regRepo.save(reg);
                continue;
            }

            // ❌ SUSPICIOUS → NO_SHOW + EMAIL
            if (level == AttendanceLevelEnum.SUSPICIOUS) {

                emailService.sendSuspiciousAttendanceEmail(
                        reg.getUser().getEmail(),
                        reg.getUser().getFullName(),
                        event
                );

                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
                regRepo.save(reg);
                continue;
            }

            // ❌ NONE → NO_SHOW
            if (level == AttendanceLevelEnum.NONE) {
                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
                regRepo.save(reg);
                continue;
            }

            // 🎯 Attendance factor
            double attendanceFactor =
                    (level == AttendanceLevelEnum.FULL) ? 2.0 : 1.0;

            long finalReward = Math.round(commit * attendanceFactor);

            if (finalReward > 0) {

                Wallet memberWallet =
                        walletService.getOrCreateUserWallet(reg.getUser());

                walletService.transferPointsWithType(
                        eventWallet,
                        memberWallet,
                        finalReward,
                        "Event reward for " + event.getName(),
                        WalletTransactionTypeEnum.BONUS_REWARD
                );

                emailService.sendEventSummaryEmail(
                        reg.getUser().getEmail(),
                        reg.getUser().getFullName(),
                        event,
                        finalReward,
                        "https://uniclub.id.vn/feedback?eventId=" + event.getEventId()
                );

                reg.setStatus(RegistrationStatusEnum.REWARDED);
                totalReward += finalReward;

            } else {
                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
            }

            regRepo.save(reg);
        }

        walletRepo.flush();
        regRepo.flush();

        // =====================================================
        // REFUND + CLOSE
        // =====================================================
        Event refreshed = eventRepo.findByIdWithCoHostRelations(event.getEventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event disappeared"));

        rewardService.autoSettleEvent(refreshed);

        Wallet refreshedWallet = refreshed.getWallet();
        refreshedWallet.setStatus(WalletStatusEnum.CLOSED);
        walletRepo.save(refreshedWallet);

        refreshed.setStatus(EventStatusEnum.COMPLETED);
        refreshed.setCompletedAt(LocalDateTime.now());
        eventRepo.save(refreshed);

        return "Event completed. Total reward " + totalReward + " pts; leftover refunded.";
    }


    private Wallet ensureEventWallet(Event event) {
        Wallet w = event.getWallet();
        if (w == null)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet not found. UniStaff must approve the event budget first.");

        if (w.getStatus() == WalletStatusEnum.CLOSED)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet is already closed.");

        return w;
    }

    @Override
    public void refundCommitPoints(User user, long points, Event event) {
        Wallet wallet = walletRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(points)
                        .type(WalletTransactionTypeEnum.REFUND_COMMIT)
                        .description("Refund commit points from cancelled event: " + event.getName())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

}
