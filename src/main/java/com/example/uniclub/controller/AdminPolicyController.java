package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminPolicyResponse;
import com.example.uniclub.service.AdminPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final AdminPolicyService policyService;

    @Operation(summary = "📋 Lấy tất cả chính sách Major & Multiplier")
    @GetMapping
    public ResponseEntity<List<AdminPolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(policyService.getAllPolicies());
    }

    @Operation(summary = "➕ Thêm hoặc cập nhật chính sách")
    @PostMapping
    public ResponseEntity<AdminPolicyResponse> savePolicy(@RequestBody AdminPolicyResponse req) {
        return ResponseEntity.ok(policyService.savePolicy(req));
    }

    @Operation(summary = "🗑️ Xóa chính sách")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.ok().build();
    }
}
