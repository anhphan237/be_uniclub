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

    // 🔹 Leader / Staff / Admin xem member theo clubId
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<?>> getMembersByClub(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersByClub(principal, clubId)));
    }

    // 🔹 Leader / Staff / Admin xem member CLB của chính mình
    @GetMapping("/my-club")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<?>> getMembersOfMyClub(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.getMembersOfMyClub(principal)));
    }

}
