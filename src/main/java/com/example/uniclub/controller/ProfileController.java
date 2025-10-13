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
 * ✅ Cho phép tất cả người dùng đăng nhập (mọi role)
 * xem và cập nhật hồ sơ cá nhân của chính mình.
 */
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserServiceImpl userService;

    // ✅ Xem thông tin profile của chính mình
    @GetMapping
    @PreAuthorize("isAuthenticated()") // 🔓 cho phép tất cả role đã đăng nhập
    public ResponseEntity<ApiResponse<User>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {

        String email = principal.getUsername();
        User profile = userService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // ✅ Cập nhật profile của chính mình
    @PutMapping
    @PreAuthorize("isAuthenticated()") // 🔓 cho phép tất cả role đã đăng nhập
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ProfileUpdateRequest req) {

        String email = principal.getUsername();
        User updated = userService.updateProfile(email, req);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
    // ✅ Cập nhật avatar URL thủ công (nếu không dùng Google)
    @PatchMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updateAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String avatarUrl) {

        String email = principal.getUsername();
        User updated = userService.updateAvatar(email, avatarUrl);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

}
