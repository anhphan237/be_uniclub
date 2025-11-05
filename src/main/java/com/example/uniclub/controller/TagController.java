package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
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
        Quản lý **Tag (nhãn)** trong hệ thống UniClub:<br>
        - Tag được dùng để phân loại sản phẩm, sự kiện, hoặc bài đăng.<br>
        - Người dùng có thể xem tất cả tag.<br>
        - Chỉ **UNIVERSITY_STAFF** có quyền tạo mới hoặc xóa tag.<br>
        - Một số **core tags** không thể bị xóa.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(
            summary = "Tạo tag mới",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Tạo một tag mới trong hệ thống nếu chưa tồn tại.<br>
                Hữu ích để phân loại các sản phẩm, sự kiện hoặc bài viết theo chủ đề.
                """
    )
    @PostMapping
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Tag>> createTag(@RequestParam String name) {
        Tag tag = tagService.createTagIfNotExists(name);
        return ResponseEntity.ok(ApiResponse.ok(tag));
    }

    @Operation(
            summary = "Lấy danh sách tất cả tags",
            description = """
                Public API — ai cũng có thể xem.<br>
                Trả về danh sách toàn bộ tag hiện có trong hệ thống, bao gồm cả tag mặc định và tag tùy chỉnh.
                """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getAllTags()));
    }

    @Operation(
            summary = "Xóa một tag (chỉ dành cho UniStaff)",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Cho phép xóa tag khỏi hệ thống nếu không phải tag lõi (core tag).<br>
                Nếu tag đang được sử dụng hoặc là core tag, hệ thống sẽ chặn thao tác xóa.
                """
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.msg("Tag deleted successfully"));
    }
}
