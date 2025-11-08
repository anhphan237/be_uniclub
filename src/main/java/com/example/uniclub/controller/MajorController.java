package com.example.uniclub.controller;

import com.example.uniclub.entity.Major;
import com.example.uniclub.service.MajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(
        name = "Major Management",
        description = """
        Quản lý ngành học trong hệ thống UniClub:<br>
        - Lấy danh sách, xem chi tiết hoặc tra cứu theo mã ngành.<br>
        - Cho phép **ADMIN** hoặc **UNIVERSITY_STAFF** thêm, sửa hoặc xóa ngành.<br>
        Dữ liệu ngành được dùng cho sinh viên, CLB và chính sách nhân điểm (Major Policy).
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/university/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    // =========================================================
    // 📋 1. GET - PUBLIC
    // =========================================================
    @Operation(
            summary = "Lấy danh sách tất cả ngành học",
            description = """
                Public API - ai cũng có thể xem.<br>
                Trả về danh sách tất cả ngành học hiện có trong hệ thống.<br>
                Dùng cho trang tạo CLB, chọn chuyên ngành hoặc đăng ký sinh viên.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách ngành học thành công")
            }
    )
    @GetMapping
    public ResponseEntity<List<Major>> getAll() {
        return ResponseEntity.ok(majorService.getAll());
    }

    // =========================================================
    // 🔍 2. GET BY ID - PUBLIC
    // =========================================================
    @Operation(
            summary = "Xem chi tiết ngành học theo ID",
            description = """
                Public API - ai cũng có thể xem.<br>
                Trả về thông tin chi tiết của một ngành học cụ thể.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết ngành học thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy ngành học")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<Major> getById(@PathVariable Long id) {
        return ResponseEntity.ok(majorService.getById(id));
    }

    // =========================================================
    // 🔎 3. GET BY CODE - PUBLIC
    // =========================================================
    @Operation(
            summary = "Tìm kiếm ngành học theo mã code",
            description = """
                Public API - ai cũng có thể xem.<br>
                Dùng khi cần tra cứu ngành học theo mã code (ví dụ: SE, AI, BA,...).
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy ngành học thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy ngành học theo mã code")
            }
    )
    @GetMapping("/code/{code}")
    public ResponseEntity<Major> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(majorService.getByMajorCode(code));
    }

    // =========================================================
    // 🟢 4. CREATE - ADMIN/STAFF
    // =========================================================
    @Operation(
            summary = "Thêm mới ngành học",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Nhập thông tin cơ bản: tên ngành, mã ngành, mô tả.<br>
                Trả về đối tượng ngành học vừa được tạo.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo ngành học thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền thêm ngành học")
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<Major> create(@RequestBody Major major) {
        return ResponseEntity.ok(majorService.create(major));
    }

    // =========================================================
    // ✏️ 5. UPDATE - ADMIN/STAFF
    // =========================================================
    @Operation(
            summary = "Cập nhật thông tin ngành học",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Cho phép chỉnh sửa tên, mô tả hoặc mã ngành.<br>
                Trả về thông tin ngành sau khi cập nhật.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật ngành học thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy ngành học")
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<Major> update(@PathVariable Long id, @RequestBody Major updatedMajor) {
        return ResponseEntity.ok(majorService.update(id, updatedMajor));
    }

    // =========================================================
    // 🗑️ 6. DELETE - ADMIN ONLY
    // =========================================================
    @Operation(
            summary = "Xoá ngành học theo ID",
            description = """
                Dành cho **ADMIN**.<br>
                Chỉ xoá được nếu ngành học không còn liên kết với sinh viên hoặc CLB nào.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Xoá ngành học thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền xoá ngành học"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy ngành học")
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorService.delete(id);
        return ResponseEntity.noContent().build();
    }
    // =========================================================
// 🎨 7. UPDATE COLOR - ADMIN/STAFF
// =========================================================
    @Operation(
            summary = "Cập nhật mã màu cho ngành học",
            description = """
            Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
            Cho phép thay đổi mã màu (colorHex) của ngành học mà không cần sửa các thông tin khác.<br>
            Ví dụ: {"colorHex": "#FF6600"}.
            """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đổi màu ngành học thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy ngành học")
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PatchMapping("/{id}/color")
    public ResponseEntity<Major> updateColor(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newColor = body.get("colorHex");
        Major existing = majorService.getById(id);
        existing.setColorHex(newColor);

        return ResponseEntity.ok(majorService.update(id, existing));
    }

}
