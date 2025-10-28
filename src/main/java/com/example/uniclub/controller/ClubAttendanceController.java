package com.example.uniclub.controller;

import com.example.uniclub.dto.request.BulkAttendanceRequest;
import com.example.uniclub.dto.request.ClubAttendanceSessionRequest;
import com.example.uniclub.enums.AttendanceStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/club-attendance")
@RequiredArgsConstructor
public class ClubAttendanceController {

    private final ClubAttendanceService attendanceService;

    // ============================================================
    // 📅 LẤY DANH SÁCH ĐIỂM DANH HÔM NAY (TỰ TẠO SESSION NẾU CHƯA CÓ)
    // ============================================================
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @GetMapping("/{clubId}/today")
    public Map<String, Object> getTodayAttendance(@PathVariable Long clubId) {
        return attendanceService.getTodayAttendance(clubId);
    }

    // ============================================================
    // 📜 XEM LỊCH SỬ ĐIỂM DANH CLB THEO NGÀY
    // ============================================================
    @PreAuthorize("hasRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    @GetMapping("/{clubId}/history")
    public Map<String, Object> getHistory(@PathVariable Long clubId,
                                          @RequestParam String date) {
        return attendanceService.getAttendanceHistory(clubId, date);
    }

    // ============================================================
    // ✅ ĐIỂM DANH 1 THÀNH VIÊN + GHI CHÚ
    // ============================================================
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark")
    public void markAttendance(@PathVariable Long sessionId,
                               @RequestParam Long membershipId,
                               @RequestParam AttendanceStatusEnum status,
                               @RequestParam(required = false) String note) {
        attendanceService.markAttendance(sessionId, membershipId, status, note);
    }

    // ============================================================
    // 🔄 CẬP NHẬT TRẠNG THÁI ĐIỂM DANH HÀNG LOẠT
    // ============================================================
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark-all")
    public void markAll(@PathVariable Long sessionId,
                        @RequestParam AttendanceStatusEnum status) {
        attendanceService.markAll(sessionId, status);
    }

    // ============================================================
    // 👤 THÀNH VIÊN XEM LỊCH SỬ ĐIỂM DANH CÁ NHÂN
    // ============================================================
    @PreAuthorize("hasRole('STUDENT','CLUB_LEADER')")
    @GetMapping("/member/{membershipId}/history")
    public Map<String, Object> getMemberHistory(@PathVariable Long membershipId) {
        return attendanceService.getMemberAttendanceHistory(membershipId);
    }

    // ============================================================
    // 🏫 UNI STAFF XEM TỔNG QUAN ĐIỂM DANH TOÀN TRƯỜNG
    // ============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/university/overview")
    public Map<String, Object> getUniversityOverview(@RequestParam(required = false) String date) {
        return attendanceService.getUniversityAttendanceOverview(date);
    }

    // ============================================================
    // 🆕 TẠO BUỔI ĐIỂM DANH MỚI (SESSION)
    // ============================================================
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PostMapping("/{clubId}/create-session")
    public Map<String, Object> createSession(@PathVariable Long clubId,
                                             @RequestBody ClubAttendanceSessionRequest req) {
        return attendanceService.createSession(clubId, req);
    }

    // ============================================================
    // 📦 ĐIỂM DANH NHIỀU THÀNH VIÊN CÙNG LÚC
    // ============================================================
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark-bulk")
    public Map<String, Object> markBulk(@PathVariable Long sessionId,
                                        @RequestBody BulkAttendanceRequest req,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        return attendanceService.markBulk(sessionId, req, user);
    }

}
