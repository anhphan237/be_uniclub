package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.TagRequest;
import com.example.uniclub.dto.response.TagResponse;
import com.example.uniclub.entity.Tag;
import com.example.uniclub.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@io.swagger.v3.oas.annotations.tags.Tag(
        name = "Tag Management",
        description = """
        Quản lý Tag trong hệ thống UniClub.<br>
        - Xem tất cả tag.<br>
        - ADMIN và UNIVERSITY_STAFF có quyền tạo, sửa, xoá.<br>
        - Core tags không thể bị xoá.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // CREATE
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Tạo tag mới",
            description = "ADMIN hoặc UNIVERSITY_STAFF có thể tạo tag mới."
    )
    public ResponseEntity<ApiResponse<Tag>> createTag(@RequestParam String name) {
        Tag tag = tagService.createTagIfNotExists(name);
        return ResponseEntity.ok(ApiResponse.ok(tag));
    }

    // GET ALL
    @GetMapping
    @Operation(
            summary = "Lấy danh sách tất cả tags",
            description = "Public API — bất kỳ ai cũng xem được."
    )
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getAllTags()));
    }

    // DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Xoá tag",
            description = """
                Chỉ ADMIN và UNIVERSITY_STAFF.<br>
                Core tags không thể bị xoá.
                """
    )
    public ResponseEntity<ApiResponse<String>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.msg("Tag deleted successfully"));
    }

    // UPDATE
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Cập nhật tag",
            description = """
                ADMIN và UNIVERSITY_STAFF có quyền sửa tag.<br>
                Core tags không được chỉnh sửa.
                """
    )
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable Long id,
            @RequestBody TagRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tagService.updateTag(id, request)));
    }
}
