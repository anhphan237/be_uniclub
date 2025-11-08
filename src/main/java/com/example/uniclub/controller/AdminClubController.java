package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminClubResponse;
import com.example.uniclub.dto.response.AdminClubStatResponse;
import com.example.uniclub.service.AdminClubService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/clubs")
@RequiredArgsConstructor
public class AdminClubController {

    private final AdminClubService adminClubService;

    @Operation(summary = "🏫 Lấy danh sách tất cả CLB (phân trang + tìm kiếm)")
    @GetMapping
    public ResponseEntity<Page<AdminClubResponse>> getAllClubs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("clubId").descending());
        return ResponseEntity.ok(adminClubService.getAllClubs(keyword, pageable));
    }

    @Operation(summary = "🔍 Xem chi tiết CLB")
    @GetMapping("/{id}")
    public ResponseEntity<AdminClubResponse> getClubDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminClubService.getClubDetail(id));
    }

    @Operation(summary = "✅ Duyệt CLB")
    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveClub(@PathVariable Long id) {
        adminClubService.approveClub(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "🚫 Tạm dừng hoạt động CLB")
    @PutMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendClub(@PathVariable Long id) {
        adminClubService.suspendClub(id);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "📊 Xem thống kê CLB")
    @GetMapping("/{id}/stats")
    public ResponseEntity<AdminClubStatResponse> getClubStats(@PathVariable Long id) {
        return ResponseEntity.ok(adminClubService.getClubStats(id));
    }

}
