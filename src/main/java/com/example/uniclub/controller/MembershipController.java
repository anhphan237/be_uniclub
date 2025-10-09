package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.MemberCreateRequest;
import com.example.uniclub.dto.response.MembershipResponse;
import com.example.uniclub.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
