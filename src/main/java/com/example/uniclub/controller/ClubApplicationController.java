package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/club-applications")
@RequiredArgsConstructor
public class ClubApplicationController {

    private final ClubApplicationService clubApplicationService;
    private final UserService userService;

    // ============================================================
    // 🟢 1. Sinh viên nộp đơn online
    // ============================================================
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> createOnline(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ClubApplicationCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.createOnline(user.getUserId(), req)
        ));
    }

    // ============================================================
    // 🟠 2. UniStaff phê duyệt hoặc từ chối đơn
    // ============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> approveClubApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails staff,
            @Valid @RequestBody ClubApplicationDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.decide(id, staff.getUserId(), req)
        ));
    }

    // ============================================================
    // 🟢 3. UniStaff tạo 2 tài khoản CLB (Leader & ViceLeader)
    // ============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping("/create-club-accounts")
    public ResponseEntity<ApiResponse<String>> createClubAccounts(
            @Valid @RequestBody CreateClubAccountsRequest request) {
        userService.createClubAccounts(request);
        return ResponseEntity.ok(ApiResponse.ok("Club accounts created successfully."));
    }

    // ============================================================
    // 🟣 4. Sinh viên xem danh sách đơn của mình
    // ============================================================
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getByUser(user.getUserId())
        ));
    }

    // ============================================================
    // 🔵 5. Xem chi tiết 1 đơn
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> getById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getById(user.getUserId(), user.getRoleName(), id)
        ));
    }

    // ============================================================
    // 🟤 6. Danh sách đơn chờ duyệt
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.getPending()));
    }

    // ============================================================
    // 🟣 7. Thống kê số lượng đơn theo trạng thái
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.getStatistics()));
    }

    // ============================================================
    // 🔵 8. Tìm kiếm đơn theo tên CLB / người nộp
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> search(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.search(keyword)));
    }

    // ============================================================
    // ⚪ 9. Lấy toàn bộ đơn (Admin / Staff)
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.getAllApplications()));
    }
}
