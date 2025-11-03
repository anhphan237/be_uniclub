package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProfileUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.dto.response.UserStatsResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.CloudinaryService;
import com.example.uniclub.service.UserStatsService;
import com.example.uniclub.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserServiceImpl userService;
    private final CloudinaryService cloudinaryService;
    private final UserStatsService userStatsService;
    // =============================================
    // 🔹 1. Xem thông tin hồ sơ cá nhân
    // =============================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {

        String email = principal.getUsername();
        UserResponse profile = userService.getProfileResponse(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // =============================================
    // 🔹 2. Cập nhật thông tin hồ sơ (full name, phone, bio, major...)
    // =============================================
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ProfileUpdateRequest req) {

        String email = principal.getUsername();
        UserResponse updated = userService.updateProfileResponse(email, req);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // =============================================
    // 🔹 3. Upload avatar qua Cloudinary
    // =============================================
    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) throws IOException {

        String email = principal.getUsername();
        String avatarUrl = cloudinaryService.uploadAvatar(file);
        UserResponse updated = userService.updateAvatarResponse(email, avatarUrl);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // =============================================
    // 🔹 4. Upload background qua Cloudinary
    // =============================================
    @PostMapping(value = "/background", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> uploadBackground(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam("file") MultipartFile file) throws IOException {

        String email = principal.getUsername();
        String backgroundUrl = cloudinaryService.uploadBackground(file);
        UserResponse updated = userService.updateBackgroundResponse(email, backgroundUrl);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
    // =============================================
    // 🔹 5. Thống kê hồ sơ người dùng
    // =============================================
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();
        UserStatsResponse stats = userStatsService.getUserStats(userId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
