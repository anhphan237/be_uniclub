package com.example.uniclub.controller;

import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.service.MultiplierPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class MultiplierPolicyController {

    private final MultiplierPolicyService service;

    // 🔹 Lấy danh sách chính sách
    @GetMapping("/{type}")
    public List<MultiplierPolicy> getPolicies(@PathVariable PolicyTargetTypeEnum type) {
        return service.getPolicies(type);
    }

    // 🔹 Cập nhật hệ số
    @PutMapping("/{id}")
    public MultiplierPolicy updateMultiplier(
            @PathVariable Long id,
            @RequestParam Double multiplier,
            @RequestParam String updatedBy) {
        return service.updatePolicy(id, multiplier, updatedBy);
    }

    // 🔹 Tạo chính sách mới
    @PostMapping
    public MultiplierPolicy create(@RequestBody MultiplierPolicy policy) {
        return service.createPolicy(policy);
    }

    // 🔹 Xóa chính sách
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deletePolicy(id);
    }
}
