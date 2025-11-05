package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.service.MultiplierPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Multiplier Policy Management",
        description = """
        Quản lý **chính sách nhân điểm thưởng (Multiplier Policy)** trong hệ thống UniClub.<br>
        - Cho phép đặt hệ số nhân điểm cho từng đối tượng (CLUB hoặc MEMBER).<br>
        - Áp dụng khi tính điểm thưởng, thưởng thêm hoặc quy đổi trong các sự kiện.<br>
        - Chỉ dành cho **UNIVERSITY_STAFF**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/university/multiplier-policies")
@RequiredArgsConstructor
public class MultiplierPolicyController {

    private final MultiplierPolicyService multiplierPolicyService;

    // ===============================================================
    // 🔹 1️⃣ LẤY TẤT CẢ POLICIES
    // ===============================================================
    @Operation(
            summary = "Lấy danh sách toàn bộ Multiplier Policies",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về danh sách toàn bộ chính sách nhân điểm hiện có, bao gồm trạng thái hiệu lực, 
                hệ số nhân và đối tượng áp dụng (CLUB / MEMBER).
                """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<List<MultiplierPolicyResponse>> getAll() {
        return ResponseEntity.ok(multiplierPolicyService.getAll());
    }

    // ===============================================================
    // 🔹 2️⃣ LẤY CHI TIẾT POLICY THEO ID
    // ===============================================================
    @Operation(
            summary = "Lấy chi tiết một chính sách nhân điểm theo ID",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về thông tin chi tiết của một policy bao gồm: tên, đối tượng áp dụng, hệ số nhân và mô tả.
                """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(multiplierPolicyService.getById(id));
    }

    // ===============================================================
    // 🔹 3️⃣ LỌC POLICY THEO LOẠI (CLUB / MEMBER)
    // ===============================================================
    @Operation(
            summary = "Lọc danh sách chính sách theo loại đối tượng",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về danh sách các chính sách hiện đang áp dụng cho loại mục tiêu cụ thể:<br>
                - `CLUB`: Chính sách nhân điểm dành cho CLB<br>
                - `MEMBER`: Chính sách nhân điểm dành cho thành viên
                """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/target/{type}")
    public ResponseEntity<List<MultiplierPolicyResponse>> getByType(@PathVariable PolicyTargetTypeEnum type) {
        return ResponseEntity.ok(multiplierPolicyService.getActiveByTargetType(type));
    }

    // ===============================================================
    // 🔹 4️⃣ TẠO MỚI POLICY
    // ===============================================================
    @Operation(
            summary = "Tạo mới chính sách nhân điểm",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Tạo mới một chính sách nhân điểm mới (Multiplier Policy) với thông tin hệ số nhân, 
                đối tượng mục tiêu và mô tả chi tiết.
                """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<MultiplierPolicyResponse> create(@RequestBody MultiplierPolicyRequest request) {
        return ResponseEntity.ok(multiplierPolicyService.create(request));
    }

    // ===============================================================
    // 🔹 5️⃣ CẬP NHẬT POLICY
    // ===============================================================
    @Operation(
            summary = "Cập nhật thông tin một chính sách nhân điểm",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Cập nhật hệ số nhân, mô tả, hoặc trạng thái hiệu lực của chính sách.<br>
                Các thay đổi chỉ có hiệu lực cho các hoạt động được tạo sau khi cập nhật.
                """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> update(
            @PathVariable Long id,
            @RequestBody MultiplierPolicyRequest request
    ) {
        return ResponseEntity.ok(multiplierPolicyService.update(id, request));
    }

    // ===============================================================
    // 🔹 6️⃣ XÓA POLICY
    // ===============================================================
    @Operation(
            summary = "Xoá chính sách nhân điểm",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Xoá một chính sách nhân điểm khỏi hệ thống nếu nó chưa được áp dụng trong sự kiện hoặc điểm thưởng hiện hành.
                """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        multiplierPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
