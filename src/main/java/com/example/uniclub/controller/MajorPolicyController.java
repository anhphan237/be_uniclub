package com.example.uniclub.controller;

import com.example.uniclub.dto.request.MajorPolicyRequest;
import com.example.uniclub.dto.response.MajorPolicyResponse;
import com.example.uniclub.service.MajorPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/university/policies")
@RequiredArgsConstructor
public class MajorPolicyController {

    private final MajorPolicyService majorPolicyService;

    // Lấy tất cả policies
    @GetMapping
    public ResponseEntity<List<MajorPolicyResponse>> getAll() {
        return ResponseEntity.ok(majorPolicyService.getAll());
    }

    // Lấy policy theo ID
    @GetMapping("/{id}")
    public ResponseEntity<MajorPolicyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(majorPolicyService.getById(id));
    }

    // Tạo mới policy
    @PostMapping
    public ResponseEntity<MajorPolicyResponse> create(@RequestBody MajorPolicyRequest request) {
        return ResponseEntity.ok(majorPolicyService.create(request));
    }

    // Cập nhật policy
    @PutMapping("/{id}")
    public ResponseEntity<MajorPolicyResponse> update(@PathVariable Long id, @RequestBody MajorPolicyRequest request) {
        return ResponseEntity.ok(majorPolicyService.update(id, request));
    }

    // Xóa policy
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
