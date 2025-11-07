package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;
import com.example.uniclub.service.MajorPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Major Policy Management",
        description = """
        Quản lý **chính sách nhân điểm (Major Policy)** theo chuyên ngành sinh viên.<br>
        - Cho phép điều chỉnh hệ số nhân điểm thưởng cho từng ngành học.<br>
        - Ảnh hưởng trực tiếp đến việc tính **điểm thưởng và quy đổi điểm sự kiện**.<br>
        - Chỉ dành cho **UNIVERSITY_STAFF**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/university/major-policies")
@RequiredArgsConstructor
public class MajorPolicyController {

    private final MajorPolicyService majorPolicyService;

    // ==========================================================
    // 🔹 1. GET ALL
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách toàn bộ Major Policies",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về danh sách toàn bộ chính sách nhân điểm hiện có trong hệ thống.<br>
                Mỗi chính sách gắn với một ngành học cụ thể.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách chính sách thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<List<MajorPolicyResponse>> getAll() {
        return ResponseEntity.ok(majorPolicyService.getAll());
    }

    // ==========================================================
    // 🔹 2. GET BY ID
    // ==========================================================
    @Operation(
            summary = "Lấy chi tiết chính sách theo ID",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về chi tiết hệ số nhân, ngành áp dụng và thời gian hiệu lực của chính sách.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết chính sách thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chính sách")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<MajorPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(majorPolicyService.getById(id));
    }

    // ==========================================================
    // 🔹 3. CREATE
    // ==========================================================
    @Operation(
            summary = "Tạo mới chính sách nhân điểm (Major Policy)",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Thêm một chính sách mới cho ngành học cụ thể, bao gồm hệ số nhân và mô tả.<br>
                Dùng khi nhà trường muốn khuyến khích ngành học nhất định tham gia CLB/Sự kiện.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo chính sách thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền tạo mới")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<MajorPolicyResponse> create(@RequestBody MajorPolicyRequest request) {
        return ResponseEntity.ok(majorPolicyService.create(request));
    }

    // ==========================================================
    // 🔹 4. UPDATE
    // ==========================================================
    @Operation(
            summary = "Cập nhật thông tin chính sách nhân điểm",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Cho phép chỉnh sửa hệ số nhân điểm, mô tả hoặc thời gian hiệu lực.<br>
                Hệ thống sẽ áp dụng chính sách mới cho các sự kiện diễn ra sau khi cập nhật.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật chính sách thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chính sách để cập nhật")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<MajorPolicyResponse> update(@PathVariable Long id, @RequestBody MajorPolicyRequest request) {
        return ResponseEntity.ok(majorPolicyService.update(id, request));
    }

    // ==========================================================
    // 🔹 5. DELETE
    // ==========================================================
    @Operation(
            summary = "Xóa chính sách nhân điểm theo ID",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Chỉ xóa được nếu chính sách chưa được áp dụng trong sự kiện hoặc điểm thưởng hiện hành.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Xóa chính sách thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền xóa"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chính sách để xóa")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Lấy danh sách policy đang hoạt động theo ngành",
            description = """
        Dành cho **UNIVERSITY_STAFF**.<br>
        Trả về danh sách các **Major Policy** đang bật (`active = true`) của ngành được chọn.<br>
        Dùng để áp dụng khi tính giới hạn CLB hoặc multiplier cho sinh viên ngành đó.
        """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "200", description = "Lấy danh sách policy đang hoạt động thành công"),
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "404", description = "Không tìm thấy policy đang hoạt động")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/active")
    public ResponseEntity<List<MajorPolicyResponse>> getActiveByMajor(@RequestParam Long majorId) {
        return ResponseEntity.ok(majorPolicyService.getActiveByMajor(majorId));
    }

}
