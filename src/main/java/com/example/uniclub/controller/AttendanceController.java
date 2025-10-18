package com.example.uniclub.controller;

import com.example.uniclub.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping("/generate/{eventId}")
    public ResponseEntity<String> generateQr(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "300") int ttlSeconds) {
        String token = attendanceService.generateEncryptedToken(eventId, Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.ok("https://uniclub-fpt.vercel.app/login?token=" + token);
    }

    @PostMapping("/checkin")
    public ResponseEntity<String> checkIn(@RequestParam("token") String encryptedToken,
                                          @AuthenticationPrincipal Jwt jwt) {
        Long studentId = Long.valueOf(jwt.getSubject()); // hoặc lấy claim riêng
        attendanceService.checkIn(encryptedToken, studentId);
        return ResponseEntity.ok("Checked-in");
    }
}
