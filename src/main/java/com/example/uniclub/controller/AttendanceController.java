package com.example.uniclub.controller;

import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final JwtUtil jwtUtil;

    @PostMapping("/generate/{eventId}")
    public ResponseEntity<String> generateQr(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "300") int ttlSeconds) {
        String token = attendanceService.generateEncryptedToken(eventId, Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.ok("https://uniclub-fpt.vercel.app/login?token=" + token);
    }

    @PostMapping("/checkin")
    public ResponseEntity<String> checkIn(@RequestParam("token") String encryptedToken,
                                          @AuthenticationPrincipal Jwt jwt,
                                          HttpServletRequest request) {
        // Lấy token từ header
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");

        // Lấy email từ token
        String email = jwtUtil.getSubject(token);

        attendanceService.checkIn(encryptedToken, email);
        return ResponseEntity.ok("Checked-in");
    }
}
