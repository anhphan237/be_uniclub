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
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/club-applications")
@RequiredArgsConstructor
public class ClubApplicationController {

    private final ClubApplicationService clubApplicationService;
    private final ClubApplicationService service;
    // ============================================================
    // 🟢 #1. Sinh viên nộp đơn online
    // ROLE: STUDENT
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
    // 🟩 #2. Staff nhập đơn offline đã được duyệt
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping("/offline")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> createOffline(
            @AuthenticationPrincipal CustomUserDetails staff,
            @Valid @RequestBody ClubApplicationOfflineRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.createOffline(staff.getUserId(), req)
        ));
    }

    // ============================================================
    // 🟦 #3. Staff lấy danh sách đơn chờ duyệt
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.getPending()));
    }

    // ============================================================
    // 🟠 #4. Staff duyệt / từ chối đơn online
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
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

    // ============================================================
    // 🟣 #5. Sinh viên xem trạng thái các đơn của chính mình
    // ROLE: STUDENT
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
    // 🔵 #6. Xem chi tiết 1 đơn bất kỳ (tùy theo quyền)
    // ROLE: ADMIN, UNIVERSITY_STAFF, STUDENT (chỉ xem đơn của mình)
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
    // 🟤 #7. Admin lọc đơn theo trạng thái (pending / approved / rejected)
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> filterByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String clubType) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.filter(status, clubType)
        ));
    }

    // ============================================================
    // ⚪ #8. Admin cập nhật ghi chú nội bộ (internal note)
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PatchMapping("/{id}/note")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> updateNote(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails staff) {
        String note = body.get("note");
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.updateNote(id, staff.getUserId(), note)
        ));
    }

    // ============================================================
    // 🟠 #9. Admin xoá 1 đơn bị lỗi hoặc nhập nhầm
    // ROLE: ADMIN
    // ============================================================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {
        clubApplicationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // ============================================================
    // 🟢 #10. Upload file minh chứng (logo, giấy tờ,...)
    // ROLE: STUDENT (khi nộp đơn), STAFF (khi nhập offline)
    // ============================================================
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','UNIVERSITY_STAFF')")
    @PostMapping("/{id}/upload")
    public ResponseEntity<ApiResponse<String>> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) {
        String url = clubApplicationService.uploadFile(id, user.getUserId(), file);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    // ============================================================
    // 🟣 #11. Admin xem thống kê tổng số đơn (theo trạng thái, tháng, loại CLB)
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.getStatistics()));
    }

    // ============================================================
    // 🔵 #12. Admin tìm kiếm đơn theo tên CLB hoặc người nộp
    // ROLE: ADMIN, UNIVERSITY_STAFF
    // ============================================================
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> search(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(clubApplicationService.search(keyword)));
    }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllApplications()));
    }


}
