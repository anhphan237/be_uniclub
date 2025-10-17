package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserStatusUpdateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ✅ Controller dành cho ADMIN hoặc STAFF quản lý người dùng
 * (Không bao gồm đăng nhập, đăng ký, reset mật khẩu hay profile cá nhân)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ Tạo user mới (ADMIN / STAFF)
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.create(req)));
    }

    // ✅ Cập nhật thông tin user
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, req)));
    }

    // ✅ Xoá user
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted successfully"));
    }

    // ✅ Lấy thông tin 1 user
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    // ✅ Lấy danh sách user (phân trang)
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(userService.list(pageable));
    }

    // ✅ Tìm kiếm user theo từ khoá (email, tên, MSSV)
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(userService.search(keyword, pageable));
    }

    // ✅ Cập nhật trạng thái hoạt động (Active / Inactive)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateStatus(id, req.active())));
    }

    // ✅ Lọc danh sách user theo vai trò (ADMIN, STAFF, STUDENT, LEADER...)
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/role/{roleName}")
    public ResponseEntity<?> getByRole(
            @PathVariable String roleName,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getByRole(roleName, pageable));
    }

    // ✅ Thống kê user theo trạng thái & vai trò
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserStatistics()));
    }

    // ✅ ADMIN ép reset mật khẩu cho user (không trùng với /auth/reset-password)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/force-reset-password")
    public ResponseEntity<ApiResponse<String>> forceResetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.msg("Password has been reset by ADMIN"));
    }
}
