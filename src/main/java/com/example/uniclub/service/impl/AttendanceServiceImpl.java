package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.FraudCaseResponse;
import com.example.uniclub.dto.response.EventStatsResponse;
import com.example.uniclub.entity.AttendanceRecord;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventRegistration;
import com.example.uniclub.entity.QRToken;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.enums.QRPhase;
import com.example.uniclub.enums.RegistrationStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.AttendanceRecordRepository;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.QRTokenRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.AttendanceService;
import com.example.uniclub.service.JwtEventTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    // === Deps ===
    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    private final EventRepository eventRepo;
    private final UserRepository userRepo;
    private final EventRegistrationRepository regRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final QRTokenRepository qrTokenRepo;
    private final JwtEventTokenService jwtEventTokenService;

    private static final int QR_EXP_SECONDS = 120;

    // =========================================================
    // 1) Leader tạo QR token cho sự kiện (START/MID/END) – TTL 30s
    // =========================================================
    @Override
    public Map<String, Object> getQrTokenForEvent(Long eventId, String phaseStr) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        QRPhase phase = QRPhase.valueOf(phaseStr.toUpperCase());
        String cacheKey = "event:qr:" + eventId + ":" + phase.name();

        // Redis cache để tránh sinh trùng trong TTL
        String cachedToken = redis.opsForValue().get(cacheKey);
        if (cachedToken != null) {
            return Map.of("token", cachedToken, "phase", phase.name(), "expiresIn", QR_EXP_SECONDS);
        }

        // ✅ Dùng JwtEventTokenServiceImpl để sinh token thống nhất
        String tokenValue = jwtEventTokenService.generateEventToken(eventId, phase.name());

        // Lưu token vào DB
        QRToken token = QRToken.builder()
                .event(event)
                .tokenValue(tokenValue)
                .phase(phase)
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusSeconds(QR_EXP_SECONDS))
                .build();
        qrTokenRepo.save(token);

        // Cache Redis
        redis.opsForValue().set(cacheKey, tokenValue, Duration.ofSeconds(QR_EXP_SECONDS));

        return Map.of("token", tokenValue, "phase", phase.name(), "expiresIn", QR_EXP_SECONDS);
    }

    // =========================================================
    // 2) Member scan QR theo pha (START / MID / END)
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

        // phải có đăng ký (mua vé) mới được điểm danh
        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You have not purchased a ticket for this event"));

        // Lấy/khởi tạo bản ghi attendance theo (user,event)
        AttendanceRecord record = attendanceRepo
                .findByUser_UserIdAndEvent_EventId(user.getUserId(), event.getEventId())
                .orElseGet(() -> AttendanceRecord.builder()
                        .user(user)
                        .event(event)
                        .build());

        LocalDateTime now = LocalDateTime.now();

        switch (phase) {
            case START -> {
                if (record.getStartCheckInTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked in (START)");
                record.setStartCheckInTime(now);

                if (reg.getStatus() != RegistrationStatusEnum.CHECKED_IN) {
                    reg.setStatus(RegistrationStatusEnum.CHECKED_IN);
                    reg.setCheckinAt(now);
                }
            }
            case MID -> {
                if (record.getMidCheckTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked in (MID)");
                if (record.getStartCheckInTime() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "You must START before MID");
                record.setMidCheckTime(now);
                if (reg.getCheckMidAt() == null) reg.setCheckMidAt(now);
            }
            case END -> {
                if (record.getEndCheckOutTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked out (END)");
                if (record.getStartCheckInTime() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "You must START before END");
                record.setEndCheckOutTime(now);
                if (reg.getCheckoutAt() == null) reg.setCheckoutAt(now);

                // Đánh giá level tự động khi END
                updateAttendanceLevel(record, reg);
            }
        }

        attendanceRepo.save(record);
        reg.setUpdatedAt(now);
        regRepo.save(reg);
    }

    // =========================================================
    // 3) API phụ: verify & save theo chuỗi level (START/MID/END)
    //    (được EventPointsServiceImpl.checkin(...) gọi)
    // =========================================================
    @Override
    @Transactional
    public void verifyAndSaveAttendance(User user, Event event, String level) {
        QRPhase phase = QRPhase.valueOf(level.toUpperCase());

        // Lấy/khởi tạo record
        AttendanceRecord record = attendanceRepo
                .findByUser_UserIdAndEvent_EventId(user.getUserId(), event.getEventId())
                .orElseGet(() -> AttendanceRecord.builder()
                        .user(user)
                        .event(event)
                        .build());

        // Đảm bảo có đăng ký
        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You have not purchased a ticket for this event"));

        LocalDateTime now = LocalDateTime.now();

        switch (phase) {
            case START -> {
                if (record.getStartCheckInTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked in (START)");
                record.setStartCheckInTime(now);
                if (reg.getStatus() != RegistrationStatusEnum.CHECKED_IN) {
                    reg.setStatus(RegistrationStatusEnum.CHECKED_IN);
                    reg.setCheckinAt(now);
                }
            }
            case MID -> {
                if (record.getMidCheckTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked in (MID)");
                if (record.getStartCheckInTime() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "You must START before MID");
                record.setMidCheckTime(now);
                if (reg.getCheckMidAt() == null) reg.setCheckMidAt(now);
            }
            case END -> {
                if (record.getEndCheckOutTime() != null)
                    throw new ApiException(HttpStatus.CONFLICT, "Already checked out (END)");
                if (record.getStartCheckInTime() == null)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "You must START before END");
                record.setEndCheckOutTime(now);
                if (reg.getCheckoutAt() == null) reg.setCheckoutAt(now);

                updateAttendanceLevel(record, reg);
            }
        }

        attendanceRepo.save(record);
        reg.setUpdatedAt(now);
        regRepo.save(reg);
    }

    // =========================================================
    // 4) Handlers START/MID/END cho interface AttendanceService
    // =========================================================
    @Override
    @Transactional
    public void handleStartCheckin(User user, Event event) {
        verifyAndSaveAttendance(user, event, QRPhase.START.name());
    }

    @Override
    @Transactional
    public void handleMidCheckin(User user, Event event) {
        verifyAndSaveAttendance(user, event, QRPhase.MID.name());
    }

    @Override
    @Transactional
    public void handleEndCheckout(User user, Event event) {
        verifyAndSaveAttendance(user, event, QRPhase.END.name());
    }

    // =========================================================
    // 5) Event stats + Fraud list (giữ nguyên cách tính)
    // =========================================================
    @Override
    public EventStatsResponse getEventStats(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);
        int total = regs.size();

        int checkin = 0, mid = 0, checkout = 0;
        int none = 0, half = 0, full = 0, suspicious = 0;

        for (EventRegistration r : regs) {
            if (r.getCheckinAt() != null) checkin++;
            if (r.getCheckMidAt() != null) mid++;
            if (r.getCheckoutAt() != null) checkout++;

            AttendanceLevelEnum lv = r.getAttendanceLevel();
            if (lv == null) {
                none++;
            } else {
                switch (lv) {
                    case FULL -> full++;
                    case HALF -> half++;
                    case SUSPICIOUS -> suspicious++;
                    default -> none++;
                }
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

    // ================== Helpers ==================
    private void validateTokenWindow(QRToken token) {
        LocalDateTime now = LocalDateTime.now();
        if (token.getValidFrom() != null && now.isBefore(token.getValidFrom()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR token not yet active");
        if (token.getValidTo() != null && now.isAfter(token.getValidTo()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "QR token expired");
    }

    private void updateAttendanceLevel(AttendanceRecord record, EventRegistration reg) {
        boolean hasStart = record.getStartCheckInTime() != null;
        boolean hasMid   = record.getMidCheckTime() != null;
        boolean hasEnd   = record.getEndCheckOutTime() != null;

        if (hasStart && hasMid && hasEnd) {
            record.setAttendanceLevel(AttendanceLevelEnum.FULL);
            reg.setAttendanceLevel(AttendanceLevelEnum.FULL);
            reg.setSuspicious(false);
            reg.setFraudReason(null);
        } else if (hasStart && hasMid) {
            record.setAttendanceLevel(AttendanceLevelEnum.HALF);
            reg.setAttendanceLevel(AttendanceLevelEnum.HALF);
            reg.setSuspicious(false);
            reg.setFraudReason(null);
        } else if (hasStart && hasEnd && !hasMid) {
            record.setAttendanceLevel(AttendanceLevelEnum.SUSPICIOUS);
            reg.setAttendanceLevel(AttendanceLevelEnum.SUSPICIOUS);
            reg.setSuspicious(true);
            reg.setFraudReason("Checked IN and OUT without MID");
        } else {
            record.setAttendanceLevel(AttendanceLevelEnum.NONE);
            reg.setAttendanceLevel(AttendanceLevelEnum.NONE);
            reg.setSuspicious(false);
            reg.setFraudReason(null);
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
    @Override
    @Transactional
    public void checkInWithToken(String eventJwtToken, String email) {
        // ⚠️ Nếu hệ thống của bạn đã bỏ cơ chế check-in cũ (eventJwtToken),
        // bạn có thể để trống hoặc throw lỗi tạm
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "Deprecated check-in method");
    }
    @Override
    @Transactional
    public String verifyAttendance(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration not found"));

        reg.setAttendanceLevel(AttendanceLevelEnum.FULL);
        reg.setUpdatedAt(LocalDateTime.now());
        regRepo.save(reg);

        return "✅ Verified full attendance (100%) for user ID " + userId;
    }

}
