package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProfileUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.dto.response.UserStatsResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.CloudinaryService;
import com.example.uniclub.service.UserStatsService;
import com.example.uniclub.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(
        name = "User Profile Management",
        description = """
        Quản lý **hồ sơ cá nhân (User Profile)** trong hệ thống UniClub:<br>
        - Xem, chỉnh sửa và cập nhật thông tin cá nhân (tên, số điện thoại, ngành học, mô tả, v.v.).<br>
        - Upload avatar và ảnh nền qua **Cloudinary**.<br>
        - Xem thống kê hoạt động người dùng (điểm, sự kiện tham gia, CLB, v.v.).<br>
        Dành cho: **mọi người dùng đã đăng nhập**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserServiceImpl userService;
    private final CloudinaryService cloudinaryService;
    private final UserStatsService userStatsService;

    // ============================================================
    // 🔹 1️⃣ XEM THÔNG TIN HỒ SƠ CÁ NHÂN
    // ============================================================
    @Operation(
            summary = "Xem thông tin hồ sơ cá nhân",
            description = """
                Dành cho **mọi người dùng đã đăng nhập**.<br>
                Trả về thông tin hồ sơ cá nhân của người dùng hiện tại, bao gồm:
                - Họ tên, email, ngành học, điểm, vai trò và thông tin CLB tham gia.
                """
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserDetails principal) {
        String email = principal.getUsername();
        UserResponse profile = userService.getProfileResponse(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // ============================================================
    // 🔹 2️⃣ CẬP NHẬT THÔNG TIN HỒ SƠ
    // ============================================================
    @Operation(
            summary = "Cập nhật thông tin hồ sơ người dùng",
            description = """
                Dành cho **người dùng đã đăng nhập**.<br>
                Cho phép chỉnh sửa các trường:
                - Họ tên, số điện thoại, bio, ngành học, giới tính, ngày sinh, v.v.
                """
    )
    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody ProfileUpdateRequest req) {
        String email = principal.getUsername();
        UserResponse updated = userService.updateProfileResponse(email, req);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // ============================================================
    // 🔹 3️⃣ UPLOAD AVATAR
    // ============================================================
    @Operation(
            summary = "Tải ảnh đại diện (avatar) lên Cloudinary",
            description = """
                Dành cho **người dùng đã đăng nhập**.<br>
                Người dùng upload ảnh mới → hệ thống lưu ảnh trên Cloudinary và cập nhật URL trong hồ sơ.
                """
    )
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

    // ============================================================
    // 🔹 4️⃣ UPLOAD BACKGROUND
    // ============================================================
    @Operation(
            summary = "Tải ảnh nền (background) lên Cloudinary",
            description = """
                Dành cho **người dùng đã đăng nhập**.<br>
                Upload ảnh nền cá nhân, thường hiển thị trên trang hồ sơ.<br>
                Lưu trên Cloudinary và cập nhật trong cơ sở dữ liệu.
                """
    )
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

    // ============================================================
    // 🔹 5️⃣ XEM THỐNG KÊ HỒ SƠ NGƯỜI DÙNG
    // ============================================================
    @Operation(
            summary = "Lấy thống kê hoạt động người dùng",
            description = """
                Dành cho **người dùng đã đăng nhập**.<br>
                Trả về thống kê chi tiết bao gồm:
                - Tổng điểm tích luỹ
                - Số CLB tham gia
                - Số sự kiện tham dự
                - Tỷ lệ điểm danh và thưởng
                """
    )
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getId();
        UserStatsResponse stats = userStatsService.getUserStats(userId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
