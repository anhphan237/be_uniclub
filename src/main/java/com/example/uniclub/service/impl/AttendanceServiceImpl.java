package com.example.uniclub.service.impl;

import com.example.uniclub.entity.AttendanceRecord;
import com.example.uniclub.entity.AttendanceToken;
import com.example.uniclub.repository.AttendanceRecordRepository;
import com.example.uniclub.repository.AttendanceTokenRepository;
import com.example.uniclub.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceTokenRepository tokenRepo;
    private final AttendanceRecordRepository recordRepo;

    @Override
    public String generateToken(Long eventId) {
        String token = UUID.randomUUID().toString();
        AttendanceToken qr = new AttendanceToken();
        qr.setEventId(eventId);
        qr.setToken(token);
        qr.setExpiredAt(LocalDateTime.now().plusMinutes(10));
        tokenRepo.save(qr);
        return token;
    }

    @Override
    public String checkIn(String token, Long studentId) {
        AttendanceToken attendanceToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (attendanceToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        if (attendanceToken.getIsUsed()) {
            throw new RuntimeException("Token already used");
        }

        boolean alreadyChecked = recordRepo.existsByEventIdAndStudentId(attendanceToken.getEventId(), studentId);
        if (alreadyChecked) {
            return "You already checked in!";
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setEventId(attendanceToken.getEventId());
        record.setStudentId(studentId);
        record.setCheckinTime(LocalDateTime.now());
        recordRepo.save(record);

        // có thể cho phép nhiều người dùng cùng token,
        // nên không set used = true nếu token dùng chung
        return "Check-in successful!";
    }
}
