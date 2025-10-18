package com.example.uniclub.service.impl;

import com.example.uniclub.entity.AttendanceRecord;
import com.example.uniclub.entity.AttendanceToken;
import com.example.uniclub.entity.User;
import com.example.uniclub.repository.AttendanceRecordRepository;
import com.example.uniclub.repository.AttendanceTokenRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.AttendanceService;
import com.example.uniclub.service.UserService;
import com.example.uniclub.util.CryptoUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceTokenRepository tokenRepo;
    private final AttendanceRecordRepository recordRepo;
    private final CryptoUtil crypto;
    private final UserService userService;

    /** Admin tạo QR: lưu rawToken + TTL, trả encryptedToken để FE nhúng vào QR URL */
    public String generateEncryptedToken(Long eventId, Duration ttl) {
        String raw = UUID.randomUUID().toString();
        AttendanceToken t = new AttendanceToken();
        t.setEventId(eventId);
        t.setRawToken(raw);
        t.setExpiredAt(LocalDateTime.now().plus(ttl));
        tokenRepo.save(t);
        return crypto.encryptToB64Url(raw);
    }

    /** Student check-in sau khi login (JWT) */
    @Transactional
    public void checkIn(String encryptedToken, String email) {
        // 1) decrypt về raw
        String raw = crypto.decryptFromB64Url(encryptedToken);

        // 2) tìm token trong DB
        AttendanceToken token = tokenRepo.findByRawToken(raw)
                .orElseThrow(() -> new IllegalStateException("Invalid token"));

        // 3) check TTL
        if (token.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }

        Long studentId = getStudentIdByEmail(email);

        // 4) ghi attendance (unique eventId, studentId)
        try {
            if (!recordRepo.existsByEventIdAndStudentId(token.getEventId(), studentId)) {
                recordRepo.save(new AttendanceRecord(token.getEventId(), studentId));
            }
        } catch (DataIntegrityViolationException ignore) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already checked in");// duplicate -> coi như đã check-in
        }
    }

    private Long getStudentIdByEmail(String email) {
        User user = userService.getByEmail(email);
        Long studentId = user.getUserId();
        return studentId;
    }
}
