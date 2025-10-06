package com.example.uniclub.controller;

import com.example.uniclub.entity.Major;
import com.example.uniclub.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/university/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    @GetMapping
    public ResponseEntity<List<Major>> getAll() {
        return ResponseEntity.ok(majorService.getAll());
    }

    @PostMapping
    public ResponseEntity<Major> create(@RequestBody Major major) {
        return ResponseEntity.ok(majorService.create(major));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        majorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
