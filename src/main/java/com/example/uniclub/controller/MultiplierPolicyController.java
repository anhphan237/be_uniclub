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
        Quản lý chính sách nhân điểm (Multiplier Policy) cho CLUB và MEMBER.<br>
        Dành cho **ADMIN** và **UNIVERSITY_STAFF**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/university/multiplier-policies")
@RequiredArgsConstructor
public class MultiplierPolicyController {

    private final MultiplierPolicyService multiplierPolicyService;

    // ===============================================================
    // 1️⃣ LẤY TẤT CẢ POLICIES
    // ===============================================================
    @Operation(summary = "Lấy danh sách toàn bộ Multiplier Policies")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<List<MultiplierPolicyResponse>> getAll() {
        return ResponseEntity.ok(multiplierPolicyService.getAll());
    }

    // ===============================================================
    // 2️⃣ LẤY CHI TIẾT POLICY THEO ID
    // ===============================================================
    @Operation(summary = "Lấy chi tiết một chính sách nhân điểm theo ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(multiplierPolicyService.getById(id));
    }

    // ===============================================================
    // 3️⃣ LỌC POLICY THEO TARGET TYPE
    // ===============================================================
    @Operation(summary = "Lọc chính sách theo loại đối tượng (CLUB / MEMBER)")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @GetMapping("/target/{type}")
    public ResponseEntity<List<MultiplierPolicyResponse>> getByType(
            @PathVariable PolicyTargetTypeEnum type
    ) {
        return ResponseEntity.ok(multiplierPolicyService.getActiveByTargetType(type));
    }

    // ===============================================================
    // 4️⃣ TẠO MỚI
    // ===============================================================
    @Operation(summary = "Tạo mới chính sách nhân điểm")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<MultiplierPolicyResponse> create(
            @RequestBody MultiplierPolicyRequest request
    ) {
        return ResponseEntity.ok(multiplierPolicyService.create(request));
    }

    // ===============================================================
    // 5️⃣ CẬP NHẬT
    // ===============================================================
    @Operation(summary = "Cập nhật thông tin một chính sách nhân điểm")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> update(
            @PathVariable Long id,
            @RequestBody MultiplierPolicyRequest request
    ) {
        return ResponseEntity.ok(multiplierPolicyService.update(id, request));
    }

    // ===============================================================
    // 6️⃣ XÓA
    // ===============================================================
    @Operation(summary = "Xoá chính sách nhân điểm")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        multiplierPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
