package com.example.uniclub.controller;

import com.example.uniclub.entity.MajorPolicy;
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

    @GetMapping
    public ResponseEntity<List<MajorPolicy>> getAll() {
        return ResponseEntity.ok(majorPolicyService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MajorPolicy> getById(@PathVariable Long id) {
        return ResponseEntity.ok(majorPolicyService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MajorPolicy> create(@RequestBody MajorPolicy policy) {
        return ResponseEntity.ok(majorPolicyService.create(policy));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MajorPolicy> update(@PathVariable Long id, @RequestBody MajorPolicy updated) {
        return ResponseEntity.ok(majorPolicyService.update(id, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
