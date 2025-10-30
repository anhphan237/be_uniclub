package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.EventStatsResponse;
import com.example.uniclub.dto.response.FraudCaseResponse;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final JwtUtil jwtUtil;

    /** 🟢 Leader màn hình sự kiện gọi định kỳ để lấy QR mới */
    @GetMapping("/qr-token/{eventId}")
    public ResponseEntity<Map<String, Object>> getQr(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "START") String phase) {
        return ResponseEntity.ok(attendanceService.getQrTokenForEvent(eventId, phase));
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
    @PostMapping("/scan")
    public ResponseEntity<String> scanQr(
            @RequestParam("token") String qrToken,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401).body("Unauthorized");

        String jwt = authHeader.substring(7);
        String email = jwtUtil.getSubject(jwt);

        attendanceService.scanEventPhase(qrToken, email);
        return ResponseEntity.ok("✅ Scan success");
    }
    @GetMapping("/stats/{eventId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<EventStatsResponse>> getStats(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getEventStats(eventId)));
    }

    @GetMapping("/fraud/{eventId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<FraudCaseResponse>>> getFraudCases(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getFraudCases(eventId)));
    }

}
