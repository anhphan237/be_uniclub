package com.example.uniclub.service.impl;

import com.example.uniclub.entity.AttendanceRecord;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventRegistration;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.AttendanceRecordRepository;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.service.AttendanceService;
import com.example.uniclub.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final JwtEventTokenServiceImpl jwtEventTokenService;
    private final StringRedisTemplate redis;
    private final EventRepository eventRepo;
    private final AttendanceRecordRepository recordRepo;
    private final EventRegistrationRepository regRepo;
    private final UserService userService;

    private static final int QR_EXP_MINUTES = 5; // QR đổi mỗi 5 phút

    // =========================================================
    // 🔹 1. Leader lấy QR token cho sự kiện
    // =========================================================
    public Map<String, String> getQrTokenForEvent(Long eventId) {
        eventRepo.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String key = "event:qr:" + eventId;
        String token = redis.opsForValue().get(key);

        if (token == null) {
            token = jwtEventTokenService.generateEventToken(eventId, QR_EXP_MINUTES);
            redis.opsForValue().set(key, token, Duration.ofMinutes(QR_EXP_MINUTES));
        }

        String qrUrl = "https://uniclub-fpt.vercel.app/checkin?eventToken=" + token;
        return Map.of("token", token, "qrUrl", qrUrl);
    }

    // =========================================================
    // 🔹 2. Member check-in bằng QR
    // =========================================================
    @Transactional
    public void checkInWithToken(String eventJwtToken, String email) {
        var jws = jwtEventTokenService.parseAndVerify(eventJwtToken); // verify signature + exp
        var claims = jws.getBody();

        if (claims.getExpiration().toInstant().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR token expired");
        }

        Long eventId = claims.get("eventId", Long.class);
        String jti = claims.getId();

        // 🔸 chống reuse cùng token
        String jtiKey = "event:jti:" + jti;
        Boolean firstUse = redis.opsForValue().setIfAbsent(jtiKey, "1", Duration.ofMinutes(QR_EXP_MINUTES));
        if (firstUse == null || !firstUse)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "QR token already used");

        // 🔸 kiểm tra user
        User user = userService.getByEmail(email);
        Long studentId = user.getUserId();

        boolean alreadyCheckedIn = recordRepo.existsByEventIdAndStudentId(eventId, studentId);
        if (alreadyCheckedIn)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already checked in");

        try {
            recordRepo.save(new AttendanceRecord(eventId, studentId));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already checked in");
        }

        // 🔸 Cập nhật level = HALF (50%)
        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration not found"));
        reg.setAttendanceLevel(AttendanceLevelEnum.HALF);
        reg.setCheckinAt(LocalDateTime.now());
        regRepo.save(reg);
    }

    // =========================================================
    // 🔹 3. Staff xác nhận đi đủ buổi (100%)
    // =========================================================
    @Transactional
    public String verifyAttendance(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Registration not found"));

        reg.setAttendanceLevel(AttendanceLevelEnum.FULL);
        reg.setUpdatedAt(LocalDateTime.now());
        regRepo.save(reg);

        return "✅ Verified full attendance (100%) for user ID " + userId;
    }
    @Override
    @Transactional
    public void verifyAndSaveAttendance(User user, Event event, String level) {
        // 🔹 Kiểm tra xem user đã đăng ký chưa
        boolean alreadyCheckedIn = recordRepo.existsByEventIdAndStudentId(event.getEventId(), user.getUserId());
        if (alreadyCheckedIn)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Already checked in for this event");

        // 🔹 Lưu bản ghi điểm danh
        AttendanceRecord record = new AttendanceRecord();
        record.setEventId(event.getEventId());
        record.setStudentId(user.getUserId());
        record.setAttendanceLevel(AttendanceLevelEnum.valueOf(level.toUpperCase()));
        recordRepo.save(record);

        System.out.printf("✅ User %s checked in event %s (level=%s)%n",
                user.getEmail(), event.getName(), level);
    }

}
