package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.UserCreateRequest;
import com.example.uniclub.dto.request.UserStatusUpdateRequest;
import com.example.uniclub.dto.request.UserUpdateRequest;
import com.example.uniclub.dto.response.UserResponse;
import com.example.uniclub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(
        name = "User Management (Admin & UniStaff)",
        description = """
        API phục vụ **ADMIN** và **UNIVERSITY_STAFF** trong việc quản lý người dùng của hệ thống UniClub.<br>
        Bao gồm các chức năng:<br>
        - Tạo, sửa, xoá, tìm kiếm và phân trang user.<br>
        - Cập nhật trạng thái hoạt động (Active/Inactive).<br>
        - Lọc theo vai trò hoặc thống kê người dùng toàn hệ thống.<br>
        - Ép reset mật khẩu từ phía ADMIN.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ============================================================
    // 🟢 1️⃣ TẠO NGƯỜI DÙNG
    // ============================================================
    @Operation(
            summary = "Tạo user mới",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Cho phép thêm người dùng mới vào hệ thống (student, staff hoặc leader).<br>
                Hệ thống sẽ tự động gán vai trò dựa trên request.
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.create(req)));
    }

    // ============================================================
    // 🟡 2️⃣ CẬP NHẬT NGƯỜI DÙNG
    // ============================================================
    @Operation(
            summary = "Cập nhật thông tin người dùng",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Cho phép chỉnh sửa thông tin cơ bản (họ tên, email, vai trò, trạng thái...).
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, req)));
    }

    // ============================================================
    // 🔴 3️⃣ XOÁ NGƯỜI DÙNG
    // ============================================================
    @Operation(
            summary = "Xoá người dùng",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Xóa người dùng khỏi hệ thống (thường chỉ nên dùng cho tài khoản test hoặc bị khóa vĩnh viễn).
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted successfully"));
    }

    // ============================================================
    // 🔍 4️⃣ LẤY THÔNG TIN 1 USER
    // ============================================================
    @Operation(
            summary = "Xem chi tiết thông tin người dùng",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về thông tin chi tiết của user (bao gồm vai trò và CLB liên kết nếu có).
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    // ============================================================
    // 📋 5️⃣ DANH SÁCH NGƯỜI DÙNG (PHÂN TRANG)
    // ============================================================
    @Operation(
            summary = "Lấy danh sách tất cả người dùng (phân trang)",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Hỗ trợ phân trang và sắp xếp theo tiêu chí mặc định.<br>
                Trả về thông tin người dùng kèm danh sách CLB tham gia (nếu có).
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.list(pageable)));
    }

    // ============================================================
    // 🔎 6️⃣ TÌM KIẾM NGƯỜI DÙNG THEO TỪ KHÓA
    // ============================================================
    @Operation(
            summary = "Tìm kiếm người dùng theo từ khóa",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Cho phép tìm kiếm user theo tên, email, mã sinh viên hoặc vai trò.
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.search(keyword, pageable)));
    }

    // ============================================================
    // 🟠 7️⃣ CẬP NHẬT TRẠNG THÁI HOẠT ĐỘNG
    // ============================================================
    @Operation(
            summary = "Cập nhật trạng thái người dùng (Active / Inactive)",
            description = """
                Dành cho **ADMIN**.<br>
                Cho phép bật/tắt tài khoản của người dùng mà không cần xóa.<br>
                Thường dùng để tạm khóa tài khoản.
                """
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateStatus(id, req.active())));
    }

    // ============================================================
    // 🔵 8️⃣ LỌC DANH SÁCH USER THEO VAI TRÒ
    // ============================================================
    @Operation(
            summary = "Lọc người dùng theo vai trò",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về danh sách người dùng thuộc vai trò được chọn (STUDENT, CLUB_LEADER, UNIVERSITY_STAFF...).
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/role/{roleName}")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getByRole(
            @PathVariable String roleName,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getByRole(roleName, pageable)));
    }

    // ============================================================
    // 📊 9️⃣ THỐNG KÊ NGƯỜI DÙNG
    // ============================================================
    @Operation(
            summary = "Thống kê tổng quan người dùng",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về thống kê tổng số người dùng, số user theo vai trò và trạng thái.
                """
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserStatistics()));
    }

    // ============================================================
    // 🔐 🔟 ADMIN ÉP RESET MẬT KHẨU
    // ============================================================
    @Operation(
            summary = "ADMIN ép reset mật khẩu người dùng",
            description = """
                Dành riêng cho **ADMIN**.<br>
                Cho phép đặt lại mật khẩu mới cho một tài khoản trong trường hợp người dùng bị mất quyền truy cập hoặc bị khóa.<br>
                ⚠️ Hành động này nên được ghi log để đảm bảo bảo mật.
                """
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/force-reset-password")
    public ResponseEntity<ApiResponse<String>> forceResetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.msg("Password has been reset by ADMIN"));
    }
}
