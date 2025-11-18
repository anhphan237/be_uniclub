package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.dto.response.MemberApplicationStatsResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.MemberApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Member Application Management",
        description = """
        Quản lý **đơn ứng tuyển thành viên CLB (Member Application)** trong hệ thống UniClub:<br>
        - Sinh viên nộp, huỷ hoặc gửi lại đơn ứng tuyển.<br>
        - Leader/Admin duyệt, từ chối hoặc ghi chú nội bộ.<br>
        - Thống kê đơn theo trạng thái hoặc thời gian.<br>
        Dành cho các vai trò: **STUDENT**, **CLUB_LEADER**, **VICE_LEADER**, **UNIVERSITY_STAFF**, **ADMIN**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member-applications")
public class MemberApplicationController {

    private final MemberApplicationService service;
    private final JwtUtil jwtUtil;

    // ==========================================================
    // 🟢 1. CREATE - Student submit application
    // ==========================================================
    @Operation(
            summary = "Sinh viên nộp đơn ứng tuyển CLB",
            description = """
                Dành cho **STUDENT**.<br>
                Sinh viên gửi đơn ứng tuyển tham gia CLB.<br>
                Hệ thống sẽ lưu đơn ở trạng thái `PENDING` để leader xét duyệt.
                """
    )
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MemberApplicationResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationCreateRequest req,
            HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        return ResponseEntity.ok(service.createByEmail(principal.getUsername(), req));
    }

    // ==========================================================
    // 🟡 2. UPDATE STATUS - Approve/Reject by Leader or Admin
    // ==========================================================
    @Operation(
            summary = "Leader hoặc Staff cập nhật trạng thái đơn ứng tuyển",
            description = """
                Dành cho **CLUB_LEADER**, **UNIVERSITY_STAFF**, hoặc **ADMIN**.<br>
                Cập nhật trạng thái đơn sang `APPROVED`, `REJECTED`, hoặc `IN_PROGRESS` kèm ghi chú (note).
                """
    )
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<MemberApplicationResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationStatusUpdateRequest req) {
        return ResponseEntity.ok(service.updateStatusByEmail(principal.getUsername(), id, req));
    }

    // ==========================================================
    // 🔵 3. GET LIST - View Applications
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách đơn ứng tuyển của người dùng hiện tại",
            description = """
                - **STUDENT**: chỉ thấy các đơn mình đã nộp.<br>
                - **CLUB_LEADER/STAFF/ADMIN**: thấy tất cả các đơn của hệ thống hoặc CLB phụ trách.
                """
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<MemberApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(service.findApplicationsByEmail(principal.getUsername()));
    }

    // ==========================================================
    // 🟣 4. GET BY CLUB
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách đơn ứng tuyển theo CLB",
            description = """
                Dành cho **CLUB_LEADER**, **UNIVERSITY_STAFF**, hoặc **ADMIN**.<br>
                Trả về toàn bộ đơn ứng tuyển thuộc CLB cụ thể.
                """
    )
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<List<MemberApplicationResponse>> getByClubId(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId) {
        return ResponseEntity.ok(service.getByClubId(principal, clubId));
    }

    // ==========================================================
    // 🟢 5. GET MY APPLICATIONS
    // ==========================================================
    @Operation(summary = "Lấy danh sách đơn ứng tuyển của chính mình (student hoặc leader)")
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.findApplicationsByEmail(principal.getUsername())));
    }

    // ==========================================================
    // 🟠 6. GET BY ID
    // ==========================================================
    @Operation(summary = "Xem chi tiết đơn ứng tuyển theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','VICE_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getApplicationById(principal, id)));
    }

    // ==========================================================
    // 🔴 7. DELETE - Student cancel
    // ==========================================================
    @Operation(summary = "Sinh viên huỷ đơn ứng tuyển của mình")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> cancelApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        service.cancelApplication(principal, id);
        return ResponseEntity.ok(ApiResponse.ok("Application cancelled successfully"));
    }

    // ==========================================================
    // 🟣 8. GET PENDING BY CLUB
    // ==========================================================
    @Operation(summary = "Lấy danh sách đơn đang chờ duyệt (pending) của CLB")
    @GetMapping("/club/{clubId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getPendingApplications(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getPendingByClub(principal, clubId)));
    }

    // ==========================================================
    // 🟩 9. APPROVE
    // ==========================================================
    @Operation(summary = "Leader/Admin duyệt đơn ứng tuyển")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> approveApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.approve(principal, id)));
    }

    // ==========================================================
    // 🟥 10. REJECT
    // ==========================================================
    @Operation(summary = "Leader/Admin từ chối đơn ứng tuyển")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> rejectApplication(
            @PathVariable Long id,
            @Valid @RequestBody MemberApplicationStatusUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.reject(principal, id, req.getNote())));
    }

    // ==========================================================
    // 📊 11. STATS BY CLUB
    // ==========================================================
    @Operation(
            summary = "Thống kê số lượng đơn ứng tuyển theo trạng thái",
            description = """
                Dành cho **ADMIN**, **UNIVERSITY_STAFF**, hoặc **CLUB_LEADER**.<br>
                Trả về số lượng đơn `PENDING`, `APPROVED`, `REJECTED`, v.v. theo từng CLB.
                """
    )
    @GetMapping("/stats/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationStatsResponse>> getClubStats(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getStatsByClub(clubId)));
    }

    // ==========================================================
    // 🟢 12. UPDATE NOTE
    // ==========================================================
    @Operation(summary = "Leader/Staff cập nhật ghi chú nội bộ cho đơn ứng tuyển")
    @PatchMapping("/{id}/note")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> updateNote(
            @PathVariable Long id,
            @RequestBody String note,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateNoteForApplication(principal, id, note)));
    }

    // ==========================================================
    // 🟣 13. FILTER BY STATUS
    // ==========================================================
    @Operation(summary = "Lọc danh sách đơn theo trạng thái (Admin/Staff)")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.ok(service.getApplicationsByStatus(status)));
    }

    // ==========================================================
    // 🟡 14. RECENT APPLICATIONS
    // ==========================================================
    @Operation(summary = "Lấy 10 đơn ứng tuyển gần nhất (Admin Dashboard)")
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getRecent() {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecentApplications()));
    }

    // ==========================================================
    // 🟢 15. DAILY STATS (7 DAYS)
    // ==========================================================
    @Operation(summary = "Thống kê đơn ứng tuyển theo ngày (7 ngày gần nhất)")
    @GetMapping("/club/{clubId}/stats/daily")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationStatsResponse>>> getDailyStats(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDailyStats(clubId)));
    }

    // ==========================================================
    // 🔵 16. GET BY APPLICANT
    // ==========================================================
    @Operation(summary = "Admin xem toàn bộ đơn ứng tuyển của một sinh viên (userId)")
    @GetMapping("/applicant/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getByApplicant(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getApplicationsByApplicant(userId)));
    }

    // ==========================================================
    // 🟠 17. RESUBMIT
    // ==========================================================
    @Operation(summary = "Sinh viên gửi lại đơn đã bị từ chối (Resubmit)")
    @PutMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<MemberApplicationResponse>> resubmit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody MemberApplicationCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.resubmitApplication(principal, id, req)));
    }

    // ==========================================================
    // 🟤 18. HANDLED APPLICATIONS
    // ==========================================================
    @Operation(summary = "Lấy danh sách đơn đã xử lý (Approved/Rejected)")
    @GetMapping("/club/{clubId}/handled")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MemberApplicationResponse>>> getHandled(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.getHandledApplications(principal, clubId)));
    }
}
