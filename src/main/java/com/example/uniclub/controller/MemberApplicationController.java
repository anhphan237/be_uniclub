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

    // ✅ Sinh viên nộp đơn
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MemberApplicationResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationCreateRequest req) {

        String email = principal.getUsername();
        MemberApplicationResponse res = service.createByEmail(email, req);
        return ResponseEntity.ok(res);
    }

    // ✅ Duyệt / từ chối đơn
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<MemberApplicationResponse> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MemberApplicationStatusUpdateRequest req) {

        String email = principal.getUsername();
        MemberApplicationResponse res = service.updateStatusByEmail(email, id, req);
        return ResponseEntity.ok(res);
    }

    // ✅ Xem danh sách đơn ứng tuyển (theo role)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT','MEMBER')")
    public ResponseEntity<List<MemberApplicationResponse>> getApplications(
            @AuthenticationPrincipal UserDetails principal) {

        String email = principal.getUsername();
        List<MemberApplicationResponse> res = service.findApplicationsByEmail(email);
        return ResponseEntity.ok(res);
    }

    // ✅ Xem danh sách đơn ứng tuyển theo ClubId (Leader, Staff, Admin)
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<List<MemberApplicationResponse>> getByClubId(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long clubId) {

        List<MemberApplicationResponse> res = service.getByClubId(principal, clubId);
        return ResponseEntity.ok(res);
    }
}
