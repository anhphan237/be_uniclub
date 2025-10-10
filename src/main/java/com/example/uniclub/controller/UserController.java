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
 * ✅ Controller cho người dùng (STUDENT / MEMBER / CLUB_LEADER)
 * - PUT /api/users/profile → cập nhật thông tin
 * - GET /api/users/profile → lấy thông tin hiện tại
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    /**
     * ✅ Cập nhật thông tin hồ sơ cá nhân
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT','MEMBER','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ProfileUpdateRequest req) {

        String email = principal.getUsername();
        User updated = userService.updateProfile(email, req);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    /**
     * ✅ Lấy thông tin hồ sơ cá nhân (hiển thị khi vào trang Profile)
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT','MEMBER','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<User>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {

        String email = principal.getUsername();
        User profile = userService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
