package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MemberApplicationCreateRequest;
import com.example.uniclub.dto.request.MemberApplicationStatusUpdateRequest;
import com.example.uniclub.dto.response.MemberApplicationResponse;
import com.example.uniclub.service.MemberApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

        // lấy email của user từ JWT principal
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
}
