package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    // ============================================================
    // 🟩 1️⃣ CLUB → MEMBERS RELATIONS
    // ============================================================

    /** 🔹 Lấy tất cả thành viên của 1 CLB */
    @GetMapping("/clubs/{clubId}/members")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getAllMembers(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersByClub(clubId)));
    }

    /** 🔹 Lấy danh sách đơn chờ duyệt của CLB */
    @GetMapping("/clubs/{clubId}/members/pending")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getPendingMembers(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getPendingMembers(clubId)));
    }

    /** 🔹 Lấy danh sách Staff của CLB */
    @GetMapping("/clubs/{clubId}/members/staff")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getStaffMembers(
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getStaffMembers(clubId)));
    }

    /** 🔹 Sinh viên tham gia CLB */
    @PostMapping("/clubs/{clubId}/join")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<MembershipResponse>> joinClub(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.joinClub(user.getId(), clubId)));
    }

    // ============================================================
    // 🟨 2️⃣ MEMBERSHIP MANAGEMENT (Admin, Leader)
    // ============================================================

    /** 🔹 Lấy danh sách thành viên theo tên Leader (Admin / Staff) */
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMembersByLeaderName(
            @RequestParam(required = false) String leaderName) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersByLeaderName(leaderName)));
    }

    /** 🔹 Duyệt đơn tham gia CLB */
    @PatchMapping("/memberships/{membershipId}/approve")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MembershipResponse>> approveMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.approveMember(membershipId, user.getId())));
    }

    /** 🔹 Từ chối đơn tham gia CLB */
    @PatchMapping("/memberships/{membershipId}/reject")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<MembershipResponse>> rejectMember(
            @PathVariable Long membershipId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.rejectMember(membershipId, user.getId(), reason)));
    }

    /** 🔹 Cập nhật vai trò của thành viên (Leader Only) */
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

    /** 🔹 Xóa hoặc hủy kích hoạt thành viên khỏi CLB */
    @DeleteMapping("/memberships/{membershipId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> removeMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        membershipService.removeMember(membershipId, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Member removed successfully")));
    }

    // ============================================================
    // 🔵 3️⃣ USER → PERSONAL MEMBERSHIPS
    // ============================================================

    /** 🔹 Xem danh sách CLB mà user hiện tại tham gia */
    @GetMapping("/users/me/clubs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyClubs(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMyMemberships(user.getId())));
    }
    @PatchMapping("/memberships/{membershipId}/kick")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> kickMember(
            @PathVariable Long membershipId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                membershipService.kickMember(user, membershipId)
        ));
    }

}
