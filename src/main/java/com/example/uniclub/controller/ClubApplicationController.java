package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/club-applications")
@RequiredArgsConstructor
public class ClubApplicationController {

    private final ClubApplicationService clubApplicationService;

    // 🟢 Sinh viên nộp đơn online
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> createOnline(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ClubApplicationCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.createOnline(user.getUserId(), req)
        ));
    }

    // 🟦 Staff nhập đơn offline đã được duyệt
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping("/offline")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> createOffline(
            @AuthenticationPrincipal CustomUserDetails staff,
            @Valid @RequestBody ClubApplicationOfflineRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.createOffline(staff.getUserId(), req)
        ));
    }

    // 🟡 Staff lấy danh sách đơn chờ duyệt
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.getPending()));
    }

    // 🔵 Staff duyệt / từ chối đơn online
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PutMapping("/{id}/decide")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> decide(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails staff,
            @Valid @RequestBody ClubApplicationDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.decide(id, staff.getUserId(), req)
        ));
    }
}
