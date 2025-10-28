package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.PointRequestCreateRequest;
import com.example.uniclub.dto.response.PointRequestResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.PointRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/point-requests")
@RequiredArgsConstructor
public class PointRequestController {

    private final PointRequestService pointRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<PointRequestResponse>> createRequest(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody PointRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.createRequest(principal, req)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<PointRequestResponse>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.getPendingRequests()));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> reviewRequest(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.msg(pointRequestService.reviewRequest(id, approve, note)));
    }
}
