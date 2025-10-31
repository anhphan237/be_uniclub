package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.entity.Tag;
import com.example.uniclub.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // 🟢 UniStaff có thể tạo tag mới
    @PostMapping
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Tag>> createTag(@RequestParam String name) {
        Tag tag = tagService.createTagIfNotExists(name);
        return ResponseEntity.ok(ApiResponse.ok(tag));
    }

    // 🟢 Lấy danh sách tất cả tag
    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getAllTags()));
    }

    // 🟢 Xoá tag (cấm xoá core tag)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.msg("Tag deleted successfully"));
    }
}
