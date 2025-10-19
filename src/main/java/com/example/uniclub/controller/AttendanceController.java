package com.example.uniclub.controller;

import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final JwtUtil jwtUtil;

    /** 🟢 Leader màn hình sự kiện gọi định kỳ để lấy QR mới */
    @GetMapping("/qr-token/{eventId}")
    public ResponseEntity<?> getQr(@PathVariable Long eventId) {
        return ResponseEntity.ok(attendanceService.getQrTokenForEvent(eventId));
    }

    /** 🟢 Member gửi check-in */
    @PostMapping("/checkin")
    public ResponseEntity<String> checkIn(@RequestParam("token") String eventJwtToken,
                                          HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401).body("Unauthorized");

        String jwt = authHeader.substring(7);
        String email = jwtUtil.getSubject(jwt);

        attendanceService.checkInWithToken(eventJwtToken, email);
        return ResponseEntity.ok("Checked-in successfully");
    }
}
