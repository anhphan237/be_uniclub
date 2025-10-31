package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.PointRequestCreateRequest;
import com.example.uniclub.dto.response.PointRequestResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.PointRequestService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/point-requests")
@RequiredArgsConstructor
public class PointRequestController {

    private final PointRequestService pointRequestService;

    /** 🟢 Club tạo request xin điểm từ UniStaff */
    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<PointRequestResponse>> createRequest(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody PointRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.createRequest(principal, req)));
    }

    /** 🟢 UniStaff xem danh sách các yêu cầu đang chờ duyệt */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<PointRequestResponse>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.getPendingRequests()));
    }

    /** 🟢 UniStaff duyệt hoặc từ chối yêu cầu điểm */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> reviewRequest(
            @PathVariable Long id,
            @RequestParam boolean approve) {
        return ResponseEntity.ok(ApiResponse.msg(pointRequestService.reviewRequest(id, approve, null)));
    }


    /** 🟢 Lấy tất cả yêu cầu điểm (phân trang) */
    @GetMapping
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<Page<PointRequestResponse>> getAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(pointRequestService.list(pageable));
    }

    /** 🟢 Lấy chi tiết 1 yêu cầu theo ID */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<PointRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.get(id)));
    }
    /** 🟢 Lấy toàn bộ yêu cầu điểm (không phân trang) */
    @GetMapping("/all")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<PointRequestResponse>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.getAllRequests()));
    }

}
