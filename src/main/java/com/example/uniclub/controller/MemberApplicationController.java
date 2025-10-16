package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.dto.response.MemberApplicationStatsResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MemberApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member-applications")
public class MemberApplicationController {

    private final MemberApplicationService service;

    // 🟢 [POST] Sinh viên nộp đơn ứng tuyển
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MemberApplicationResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationCreateRequest req) {
        return ResponseEntity.ok(service.createByEmail(principal.getUsername(), req));
    }

    // 🟡 [PUT] Leader/Admin cập nhật trạng thái đơn
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<MemberApplicationResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationStatusUpdateRequest req) {
        return ResponseEntity.ok(service.updateStatusByEmail(principal.getUsername(), id, req));
    }

    // 🔵 [GET] Lấy danh sách đơn (student → của mình, leader → tất cả)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<MemberApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.findApplicationsByEmail(principal.getUsername()));
    }

    // 🟣 [GET] Xem đơn theo CLB
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<List<MemberApplicationResponse>> getByClubId(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId) {
        return ResponseEntity.ok(service.getByClubId(principal, clubId));
    }

    // 🟢 [GET] Xem đơn của chính mình
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.findApplicationsByEmail(principal.getUsername())));
    }

    // 🟠 [GET] Xem chi tiết 1 đơn
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','VICE_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getApplicationById(principal, id)));
    }

    // 🔴 [DELETE] Sinh viên hủy đơn của mình
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> cancelApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        service.cancelApplication(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Application cancelled successfully"));
    }

    // 🟣 [GET] Lấy danh sách đơn pending của CLB
    @GetMapping("/club/{clubId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getPendingApplications(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPendingByClub(principal, clubId)));
    }

    // 🟩 [PUT] Duyệt đơn
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> approveApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.approve(principal, id)));
    }

    // 🟥 [PUT] Từ chối đơn
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody MemberApplicationStatusUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.reject(principal, id, req.getNote())));
    }

    // 📊 [GET] Thống kê đơn theo trạng thái
    @GetMapping("/stats/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationStatsResponse>> getClubStats(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getStatsByClub(clubId)));
    }

    // 🟢 [PATCH] Cập nhật ghi chú nội bộ
    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> updateNote(
            @PathVariable Long id,
            @RequestBody String note,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateNoteForApplication(principal, id, note)));
    }

    // 🟣 [GET] Lọc đơn theo trạng thái (Admin)
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.ok(service.getApplicationsByStatus(status)));
    }

    // 🟡 [GET] 10 đơn gần nhất (Admin dashboard)
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecentApplications()));
    }

    // 🟢 [GET] Thống kê đơn theo ngày (7 ngày gần nhất)
    @GetMapping("/club/{clubId}/stats/daily")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationStatsResponse>>> getDailyStats(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDailyStats(clubId)));
    }

    // 🔵 [GET] Admin xem đơn theo userId
    @GetMapping("/applicant/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getByApplicant(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getApplicationsByApplicant(userId)));
    }

    // 🟠 [PUT] Sinh viên gửi lại đơn bị từ chối
    @PutMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> resubmit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody MemberApplicationCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.resubmitApplication(principal, id, req)));
    }

    // 🟤 [GET] Đơn đã được xử lý (Approved hoặc Rejected)
    @GetMapping("/club/{clubId}/handled")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getHandled(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getHandledApplications(principal, clubId)));
    }

}
