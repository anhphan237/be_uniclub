package com.example.uniclub.controller;

import com.example.uniclub.enums.AttendanceStatusEnum;
import com.example.uniclub.service.ClubAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/club-attendance")
@RequiredArgsConstructor
public class ClubAttendanceController {

    private final ClubAttendanceService attendanceService;

    /** Lấy danh sách điểm danh hôm nay (tự tạo session nếu chưa có). */
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @GetMapping("/{clubId}/today")
    public Map<String, Object> getTodayAttendance(@PathVariable Long clubId) {
        return attendanceService.getTodayAttendance(clubId);
    }

    /** Xem lịch sử điểm danh CLB theo ngày. */
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    @GetMapping("/{clubId}/history")
    public Map<String, Object> getHistory(@PathVariable Long clubId,
                                          @RequestParam String date) {
        return attendanceService.getAttendanceHistory(clubId, date);
    }

    /** Điểm danh 1 thành viên + ghi chú. */
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark")
    public void markAttendance(@PathVariable Long sessionId,
                               @RequestParam Long membershipId,
                               @RequestParam AttendanceStatusEnum status,
                               @RequestParam(required = false) String note) {
        attendanceService.markAttendance(sessionId, membershipId, status, note);
    }

    /** Cập nhật trạng thái điểm danh hàng loạt. */
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @PutMapping("/{sessionId}/mark-all")
    public void markAll(@PathVariable Long sessionId,
                        @RequestParam AttendanceStatusEnum status) {
        attendanceService.markAll(sessionId, status);
    }

    /** Thành viên xem lịch sử điểm danh cá nhân. */
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER')")
    @GetMapping("/member/{membershipId}/history")
    public Map<String, Object> getMemberHistory(@PathVariable Long membershipId) {
        return attendanceService.getMemberAttendanceHistory(membershipId);
    }

    /** University Staff xem tổng quan điểm danh toàn CLB. */
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/university/overview")
    public Map<String, Object> getUniversityOverview(@RequestParam(required = false) String date) {
        return attendanceService.getUniversityAttendanceOverview(date);
    }
}