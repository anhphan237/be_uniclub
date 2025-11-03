package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MultiplierPolicyRequest;
import com.example.uniclub.dto.response.MultiplierPolicyResponse;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.service.MultiplierPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎯 Controller: Quản lý chính sách nhân điểm thưởng (Multiplier Policy)
 * - Dành riêng cho vai trò UNIVERSITY_STAFF
 * - API prefix: /api/university/multiplier-policies
 */
@RestController
@RequestMapping("/api/university/multiplier-policies")
@RequiredArgsConstructor
public class MultiplierPolicyController {

    private final MultiplierPolicyService multiplierPolicyService;

    // ===============================================================
    // 🔹 1️⃣ Lấy tất cả policies
    // ===============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<List<MultiplierPolicyResponse>> getAll() {
        return ResponseEntity.ok(multiplierPolicyService.getAll());
    }

    // ===============================================================
    // 🔹 2️⃣ Lấy chi tiết 1 policy theo ID
    // ===============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(multiplierPolicyService.getById(id));
    }

    // ===============================================================
    // 🔹 3️⃣ Lọc policy theo loại (CLUB hoặc MEMBER)
    // ===============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/target/{type}")
    public ResponseEntity<List<MultiplierPolicyResponse>> getByType(@PathVariable PolicyTargetTypeEnum type) {
        return ResponseEntity.ok(multiplierPolicyService.getActiveByTargetType(type));
    }

    // ===============================================================
    // 🔹 4️⃣ Tạo mới policy
    // ===============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<MultiplierPolicyResponse> create(@RequestBody MultiplierPolicyRequest request) {
        return ResponseEntity.ok(multiplierPolicyService.create(request));
    }

    // ===============================================================
    // 🔹 5️⃣ Cập nhật policy
    // ===============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<MultiplierPolicyResponse> update(
            @PathVariable Long id,
            @RequestBody MultiplierPolicyRequest request
    ) {
        return ResponseEntity.ok(multiplierPolicyService.update(id, request));
    }

    // ===============================================================
    // 🔹 6️⃣ Xóa policy
    // ===============================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        multiplierPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
