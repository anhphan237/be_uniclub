package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.AdminPolicyResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.AdminPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final AdminPolicyService adminPolicyService;

    @Operation(summary = "Lấy danh sách tất cả multiplier policies (CLUB / MEMBER)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminPolicyResponse>>> getAllPolicies() {
        return ResponseEntity.ok(ApiResponse.ok(adminPolicyService.getAllPolicies()));
    }

    @Operation(summary = "Tạo mới hoặc cập nhật multiplier policy")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminPolicyResponse>> savePolicy(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestBody AdminPolicyResponse req
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(adminPolicyService.savePolicy(req, me.getUsername()))
        );
    }


    @Operation(summary = "Xóa multiplier policy theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePolicy(@PathVariable Long id) {
        adminPolicyService.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted successfully"));
    }
    @Operation(summary = "Chỉnh sửa hệ số multiplier của policy")
    @PatchMapping("/{id}/multiplier")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<AdminPolicyResponse>> updateMultiplier(
            @PathVariable Long id,
            @RequestParam Double newMultiplier,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminPolicyService.updateMultiplier(id, newMultiplier, user.getUsername())
        ));
    }
    @Operation(summary = "Lấy chi tiết một multiplier policy theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<AdminPolicyResponse>> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminPolicyService.getPolicyById(id)));
    }

}
