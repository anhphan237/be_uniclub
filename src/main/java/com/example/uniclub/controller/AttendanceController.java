package com.example.uniclub.controller;

import com.example.uniclub.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @PostMapping("/generate/{eventId}")
    public ResponseEntity<String> generateQr(@PathVariable Long eventId) {
        String token = attendanceService.generateToken(eventId);
        return ResponseEntity.ok("https://uniclub-fpt.vercel.app/login?token=" + token);
    }

    @PostMapping("/checkin")
    public ResponseEntity<String> checkIn(@RequestParam String token, @RequestParam Long studentId) {
        String result = attendanceService.checkIn(token, studentId);
        return ResponseEntity.ok(result);
    }
}
