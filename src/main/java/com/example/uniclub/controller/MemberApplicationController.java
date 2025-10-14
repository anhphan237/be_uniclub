package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.MemberApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member-applications")
public class MemberApplicationController {

    private final MemberApplicationService service;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MemberApplicationResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationCreateRequest req) {
        String email = principal.getUsername();
        return ResponseEntity.ok(service.createByEmail(email, req));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<MemberApplicationResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationStatusUpdateRequest req) {
        String email = principal.getUsername();
        return ResponseEntity.ok(service.updateStatusByEmail(email, id, req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<MemberApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal.getUsername();
        return ResponseEntity.ok(service.findApplicationsByEmail(email));
    }

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<List<MemberApplicationResponse>> getByClubId(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId) {
        return ResponseEntity.ok(service.getByClubId(principal, clubId));
    }
}
