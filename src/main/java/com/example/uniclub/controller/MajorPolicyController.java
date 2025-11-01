package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;
import com.example.uniclub.service.MajorPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🎓 MajorPolicyController
 * Quản lý chính sách thưởng điểm theo ngành học (Major Policy)
 * Chỉ dành cho UNIVERSITY_STAFF.
 *
 * API Base Path: /api/admin/major-policies
 */
@RestController
@RequestMapping("/api/university/major-policies")
@RequiredArgsConstructor
public class MajorPolicyController {

    private final MajorPolicyService majorPolicyService;

    // ==========================================================
    // 🧩 1️⃣ Lấy danh sách tất cả Major Policies
    // GET /api/admin/major-policies
    // Quyền: UNIVERSITY_STAFF
    // ==========================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping
    public ResponseEntity<List<MajorPolicyResponse>> getAll() {
        return ResponseEntity.ok(majorPolicyService.getAll());
    }

    // ==========================================================
    // 🧩 2️⃣ Lấy chi tiết 1 Policy theo ID
    // GET /api/admin/major-policies/{id}
    // Quyền: UNIVERSITY_STAFF
    // ==========================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @GetMapping("/{id}")
    public ResponseEntity<MajorPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(majorPolicyService.getById(id));
    }

    // ==========================================================
    // 🧩 3️⃣ Tạo mới Policy
    // POST /api/admin/major-policies
    // Quyền: UNIVERSITY_STAFF
    // ==========================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<MajorPolicyResponse> create(@RequestBody MajorPolicyRequest request) {
        return ResponseEntity.ok(majorPolicyService.create(request));
    }

    // ==========================================================
    // 🧩 4️⃣ Cập nhật Policy
    // PUT /api/admin/major-policies/{id}
    // Quyền: UNIVERSITY_STAFF
    // ==========================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<MajorPolicyResponse> update(@PathVariable Long id, @RequestBody MajorPolicyRequest request) {
        return ResponseEntity.ok(majorPolicyService.update(id, request));
    }

    // ==========================================================
    // 🧩 5️⃣ Xóa Policy
    // DELETE /api/admin/major-policies/{id}
    // Quyền: UNIVERSITY_STAFF
    // ==========================================================
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
