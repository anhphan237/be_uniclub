package com.example.uniclub.controller;



import com.example.uniclub.dto.response.AdminUserResponse;
import com.example.uniclub.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "👥 Lấy danh sách tất cả người dùng (phân trang + tìm kiếm)")
    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").descending());
        return ResponseEntity.ok(adminUserService.getAllUsers(keyword, pageable));
    }

    @Operation(summary = "🔍 Xem chi tiết thông tin người dùng")
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserDetail(id));
    }

    @Operation(summary = "🚫 Khóa tài khoản người dùng")
    @PutMapping("/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long id) {
        adminUserService.banUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "✅ Mở khóa tài khoản người dùng")
    @PutMapping("/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long id) {
        adminUserService.unbanUser(id);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "🧩 Admin đổi role của user")
    @PutMapping("/{id}/role")
    public ResponseEntity<Void> changeUserRole(
            @PathVariable Long id,
            @RequestParam String roleName // ví dụ: ROLE_STAFF, ROLE_CLUB_LEADER, ROLE_USER
    ) {
        adminUserService.changeUserRole(id, roleName);
        return ResponseEntity.ok().build();
    }

}
