package com.example.uniclub.service.impl;

import com.example.uniclub.entity.AttendanceRecord;
import com.example.uniclub.entity.User;
import com.example.uniclub.repository.AttendanceRecordRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final JwtEventTokenServiceImpl jwtEventTokenService;
    private final StringRedisTemplate redis;
    private final EventRepository eventRepo;
    private final AttendanceRecordRepository recordRepo;
    private final UserService userService;

    private static final int QR_EXP_MINUTES = 5; // QR đổi mỗi 5 phút

    /** 🟢 Leader lấy token QR (regen định kỳ hoặc cache nếu còn hạn) */
    public Map<String, String> getQrTokenForEvent(Long eventId) {
        // Check event tồn tại
        eventRepo.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String key = "event:qr:" + eventId;
        String token = redis.opsForValue().get(key);

        // Nếu token hết hạn → sinh mới
        if (token == null) {
            token = jwtEventTokenService.generateEventToken(eventId, QR_EXP_MINUTES);
            redis.opsForValue().set(key, token, Duration.ofMinutes(QR_EXP_MINUTES));
        }

        String qrUrl = "https://uniclub.vn/checkin?eventToken=" + token;
        return Map.of("token", token, "qrUrl", qrUrl);
    }

    /** 🟢 Member check-in sau khi login */
    @Transactional
    public void checkInWithToken(String eventJwtToken, String email) {
        var jws = jwtEventTokenService.parseAndVerify(eventJwtToken); // verify signature + exp
        var claims = jws.getBody();

        if (claims.getExpiration().toInstant().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR token expired");
        }

        Long eventId = claims.get("eventId", Long.class);
        String jti = claims.getId();

        // 🔹 chống reuse cùng token
        String jtiKey = "event:jti:" + jti;
        Boolean firstUse = redis.opsForValue().setIfAbsent(jtiKey, "1", Duration.ofMinutes(QR_EXP_MINUTES));
        if (firstUse == null || !firstUse)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "QR token already used");

        // 🔹 kiểm tra user đã điểm danh chưa
        User user = userService.getByEmail(email);
        Long studentId = user.getUserId();

        boolean alreadyCheckedIn = recordRepo.existsByEventIdAndStudentId(eventId, studentId);
        if (alreadyCheckedIn)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already checked in");

        // 🔹 lưu record
        try {
            recordRepo.save(new AttendanceRecord(eventId, studentId));
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already checked in");
        }
    }
}
