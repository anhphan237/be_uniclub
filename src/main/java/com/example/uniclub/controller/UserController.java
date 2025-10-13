package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ Controller dành cho ADMIN hoặc STAFF quản lý người dùng
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ Tạo user mới (ADMIN/STAFF)
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

    // ✅ Lấy thông tin 1 user
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    // ✅ Lấy danh sách user
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(userService.list(pageable));
    }

    // ✅ Xoá user
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted successfully"));
    }
}
