package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProfileUpdateRequest;
import com.example.uniclub.entity.User;
import com.example.uniclub.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ Controller cho STUDENT / MEMBER / CLUB_LEADER
 * - Cho phép người dùng xem và cập nhật hồ sơ cá nhân của chính mình
 */
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserServiceImpl userService;

    // ✅ Lấy thông tin profile của chính mình
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT','MEMBER','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<User>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {

        String email = principal.getUsername();
        User profile = userService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // ✅ Cập nhật profile (phone, major, bio)
    @PutMapping
    @PreAuthorize("hasAnyRole('STUDENT','MEMBER','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ProfileUpdateRequest req) {

        String email = principal.getUsername();
        User updated = userService.updateProfile(email, req);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}
