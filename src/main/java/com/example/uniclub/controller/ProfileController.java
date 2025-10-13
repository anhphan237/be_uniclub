package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProfileUpdateRequest;
import com.example.uniclub.entity.User;
import com.example.uniclub.service.CloudinaryService;
import com.example.uniclub.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * ✅ Quản lý hồ sơ cá nhân (Profile Controller)
 * Cho phép người dùng đã đăng nhập xem, chỉnh sửa và cập nhật avatar
 */
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserServiceImpl userService;
    private final CloudinaryService cloudinaryService;

    // =============================================
    // 🔹 1. Xem thông tin hồ sơ cá nhân
    // =============================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {

        String email = principal.getUsername();
        User profile = userService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // =============================================
    // 🔹 2. Cập nhật thông tin hồ sơ (full name, phone, bio, major...)
    // =============================================
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ProfileUpdateRequest req) {

        String email = principal.getUsername();
        User updated = userService.updateProfile(email, req);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // =============================================
    // 🔹 3. Upload avatar qua Cloudinary
    // =============================================
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> uploadAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) throws IOException {

        String email = principal.getUsername();
        String avatarUrl = cloudinaryService.uploadAvatar(file);
        User updated = userService.updateAvatar(email, avatarUrl);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // =============================================
    // 🔹 4. Cập nhật avatar URL thủ công (ví dụ dán link sẵn)
    // =============================================
    @PatchMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updateAvatarManual(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String avatarUrl) {

        String email = principal.getUsername();
        User updated = userService.updateAvatar(email, avatarUrl);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}
