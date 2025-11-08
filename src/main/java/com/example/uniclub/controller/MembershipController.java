package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ClubLeaveRequest;
import com.example.uniclub.dto.response.ClubLeaveRequestResponse;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.entity.ClubLeaveRequestEntity;
import com.example.uniclub.enums.LeaveRequestStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(
        name = "Membership Management",
        description = """
        Quản lý **quan hệ thành viên (Membership)** giữa sinh viên và CLB trong hệ thống UniClub:<br>
        - Sinh viên tham gia, rời khỏi, hoặc xem danh sách CLB của mình.<br>
        - Leader/Vice Leader quản lý danh sách thành viên CLB (duyệt, phân vai, xoá, kick).<br>
        - Staff/Admin theo dõi toàn bộ membership trong hệ thống.<br>
        Dành cho: **STUDENT**, **CLUB_LEADER**, **VICE_LEADER**, **UNIVERSITY_STAFF**, **ADMIN**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    // ============================================================
    // 🟩 1️⃣ CLUB → MEMBERS RELATIONS
    // ============================================================

    @Operation(
            summary = "Lấy danh sách thành viên của CLB",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER**, hoặc **STUDENT**.<br>
                Trả về danh sách toàn bộ thành viên hiện có trong CLB (bao gồm leader, vice, staff, member).
                """
    )
    @GetMapping("/clubs/{clubId}/members")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getAllMembers(@PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersByClub(clubId)));
    }

    @Operation(
            summary = "Lấy danh sách đơn tham gia đang chờ duyệt của CLB",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Trả về danh sách các sinh viên có đơn đang ở trạng thái `PENDING`.
                """
    )
    @GetMapping("/clubs/{clubId}/members/pending")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getPendingMembers(@PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getPendingMembers(clubId)));
    }

    @Operation(
            summary = "Lấy danh sách Staff của CLB",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Trả về danh sách các thành viên có vai trò STAFF trong CLB.
                """
    )
    @GetMapping("/clubs/{clubId}/members/staff")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getStaffMembers(@PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getStaffMembers(clubId)));
    }

    @Operation(
            summary = "Sinh viên tham gia CLB",
            description = """
                Dành cho **STUDENT**.<br>
                Sinh viên gửi yêu cầu tham gia CLB cụ thể, đơn sẽ được duyệt bởi Leader.
                """
    )
    @PostMapping("/clubs/{clubId}/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<MembershipResponse>> joinClub(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.joinClub(user.getId(), clubId)));
    }

    // ============================================================
    // 🟨 2️⃣ MEMBERSHIP MANAGEMENT (Leader/Admin)
    // ============================================================

    @Operation(
            summary = "Lấy danh sách thành viên theo tên Leader (Admin/Staff)",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Tìm kiếm danh sách thành viên CLB dựa theo tên Leader.
                """
    )
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMembersByLeaderName(
            @RequestParam(required = false) String leaderName) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersByLeaderName(leaderName)));
    }

    @Operation(
            summary = "Leader/Vice Leader duyệt thành viên mới",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Duyệt yêu cầu tham gia CLB, chuyển trạng thái thành viên sang `APPROVED`.
                """
    )
    @PatchMapping("/memberships/{membershipId}/approve")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MembershipResponse>> approveMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.approveMember(membershipId, user.getId())));
    }

    @Operation(
            summary = "Leader/Vice Leader từ chối đơn tham gia CLB",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Từ chối đơn của sinh viên, có thể ghi lý do từ chối (reason).
                """
    )
    @PatchMapping("/memberships/{membershipId}/reject")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MembershipResponse>> rejectMember(
            @PathVariable Long membershipId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.rejectMember(membershipId, user.getId(), reason)));
    }

    @Operation(
            summary = "Leader cập nhật vai trò của thành viên",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Cập nhật vai trò của thành viên trong CLB (`MEMBER`, `STAFF`, `VICE_LEADER`, ...).
                """
    )
    @PutMapping("/memberships/{membershipId}/role")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<MembershipResponse>> updateRole(
            @PathVariable Long membershipId,
            @RequestParam String newRole,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                membershipService.updateRole(membershipId, user.getId(), newRole)
        ));
    }

    @Operation(
            summary = "Leader xoá hoặc huỷ kích hoạt thành viên khỏi CLB",
            description = """
                Dành cho **CLUB_LEADER**.<br>
                Xoá thành viên ra khỏi CLB (ví dụ: vi phạm quy định hoặc nghỉ hoạt động).
                """
    )
    @DeleteMapping("/memberships/{membershipId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> removeMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        membershipService.removeMember(membershipId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Member removed successfully")));
    }

    @Operation(
            summary = "Leader/Vice Leader kick thành viên khỏi CLB",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Loại bỏ thành viên ngay lập tức khỏi CLB mà không cần qua trạng thái pending.
                """
    )
    @PatchMapping("/memberships/{membershipId}/kick")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> kickMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.kickMember(user, membershipId)));
    }

    // ============================================================
    // 🔵 3️⃣ USER → PERSONAL MEMBERSHIPS
    // ============================================================

    @Operation(
            summary = "Xem danh sách CLB mà người dùng hiện tại tham gia",
            description = """
                Dành cho **bất kỳ người dùng đã đăng nhập**.<br>
                Trả về danh sách tất cả CLB mà user hiện đang là thành viên (APPROVED).
                """
    )
    @GetMapping("/users/me/clubs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyClubs(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMyMemberships(user.getId())));
    }
    @Operation(summary = "Member gửi yêu cầu rời CLB (chờ Leader duyệt)")
    @PostMapping("/clubs/{clubId}/leave-request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> requestLeave(
            @PathVariable Long clubId,
            @RequestBody(required = false) ClubLeaveRequest body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String reason = (body == null) ? null : body.getReason();
        return ResponseEntity.ok(ApiResponse.ok(
                membershipService.requestLeave(user.getId(), clubId, reason)
        ));
    }

    @Operation(summary = "Leader approves/rejects a member's leave request")
    @PutMapping("/clubs/leave-request/{requestId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> reviewLeaveRequest(
            @PathVariable Long requestId,
            @Parameter(
                    description = "Action type (choose APPROVED or REJECTED)",
                    required = true,
                    example = "APPROVED"
            )
            @RequestParam LeaveRequestStatusEnum action,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                membershipService.reviewLeaveRequest(requestId, user.getId(), action.name())
        ));
    }


    @Operation(
            summary = "Thống kê nhanh – Số CLB & Sự kiện đã tham gia",
            description = """
            Dành cho **STUDENT** hoặc **CLUB_LEADER**.<br>
            Trả về tổng số CLB đang tham gia và số sự kiện đã tham gia (được duyệt).
            """
    )
    @GetMapping("/member/overview")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberOverview(
            @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> data = membershipService.getMemberOverview(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
    @Operation(summary = "Kiểm tra trạng thái thành viên của user trong CLB")
    @GetMapping("/clubs/{clubId}/membership/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMembershipStatus(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {

        boolean active = membershipService.isActiveMember(user.getId(), clubId);
        boolean joined = membershipService.isMemberOfClub(user.getId(), clubId);

        String status = active ? "ACTIVE" : (joined ? "PENDING_OR_APPROVED" : "NOT_JOINED");

        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", status)));
    }
    @Operation(
            summary = "Leader xem tất cả yêu cầu rời CLB của câu lạc bộ mình.",
            description = "Trả về toàn bộ danh sách yêu cầu rời CLB (bao gồm các trạng thái: ĐANG CHỜ DUYỆT, ĐÃ DUYỆT, và TỪ CHỐI). Chỉ dành cho Leader."
    )
    @PreAuthorize("hasRole('CLUB_LEADER')")
    @GetMapping("/clubs/{clubId}/leave-requests")
    public ResponseEntity<ApiResponse<List<ClubLeaveRequestResponse>>> getLeaveRequestsByClub(
            @PathVariable Long clubId,
            @RequestParam(required = false) LeaveRequestStatusEnum status,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var result = (status == null)
                ? membershipService.getLeaveRequestsByClub(clubId, user.getId())
                : membershipService.getLeaveRequestsByClubAndStatus(clubId, user.getId(), status);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
