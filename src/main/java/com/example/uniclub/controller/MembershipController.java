package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.MemberCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping
    public ResponseEntity<ApiResponse<MembershipResponse>> create(@Valid @RequestBody MemberCreateRequest req){
        return ResponseEntity.ok(ApiResponse.ok(membershipService.create(req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        membershipService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // 🔹 Leader xem member theo clubId (chỉ được xem CLB mình quản lý)
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<?>> getMembersByClub(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersByClub(principal, clubId)));
    }

    // 🔹 Leader xem member CLB của chính mình (khỏi truyền clubId)
    @GetMapping("/my-club")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<?>> getMembersOfMyClub(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersOfMyClub(principal)));
    }
}
