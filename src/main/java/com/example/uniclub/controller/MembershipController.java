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
@RequestMapping("/api/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    // 🔹 Lấy danh sách CLB mà user đã tham gia
    @GetMapping("/my-clubs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyClubs(
            @AuthenticationPrincipal CustomUserDetails user) {

        List<MembershipResponse> memberships = membershipService.getMyMemberships(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(memberships));
    }

    // 🔹 Kiểm tra xem user có là member của CLB cụ thể không
    @GetMapping("/check/{clubId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkMembership(
            @PathVariable Long clubId,
            @AuthenticationPrincipal CustomUserDetails user) {

        boolean isMember = membershipService.isMemberOfClub(user.getId(), clubId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("isMember", isMember)));
    }
}
