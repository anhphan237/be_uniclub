package com.example.uniclub.controller;

import com.example.uniclub.entity.Major;
import com.example.uniclub.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/university/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    // ✅ Public - ai cũng xem được
    @GetMapping
    public ResponseEntity<List<Major>> getAll() {
        return ResponseEntity.ok(majorService.getAll());
    }

    // ✅ Public - xem chi tiết
    @GetMapping("/{id}")
    public ResponseEntity<Major> getById(@PathVariable Long id) {
        return ResponseEntity.ok(majorService.getById(id));
    }

    // ✅ Public - xem theo code
    @GetMapping("/code/{code}")
    public ResponseEntity<Major> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(majorService.getByMajorCode(code));
    }

    // 🔒 Chỉ ADMIN hoặc STAFF mới được thêm
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PostMapping
    public ResponseEntity<Major> create(@RequestBody Major major) {
        return ResponseEntity.ok(majorService.create(major));
    }

    // 🔒 Chỉ ADMIN hoặc STAFF mới được sửa
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<Major> update(@PathVariable Long id, @RequestBody Major updatedMajor) {
        return ResponseEntity.ok(majorService.update(id, updatedMajor));
    }

    // 🔒 Chỉ ADMIN mới được xóa
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
