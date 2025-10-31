package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.Tag;
import com.example.uniclub.service.ProductService;
import com.example.uniclub.service.ProductMediaService;
import com.example.uniclub.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubs/{clubId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMediaService productMediaService;
    private final TagService tagService;

    // 🟢 [1] Tạo sản phẩm mới trong kho CLB
    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @PathVariable Long clubId,
            @Valid @RequestBody ProductCreateRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.create(req, clubId)));
    }

    // 🟢 [2] Lấy chi tiết sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.get(id)));
    }

    // 🟢 [3] Lấy danh sách sản phẩm CLB (phân trang)
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(@PathVariable Long clubId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.list(pageable)));
    }

    // 🟡 [4] Cập nhật tồn kho sản phẩm
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateStock(id, stock)));
    }

    // 🔴 [5] Xoá sản phẩm
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // === 🟦 MEDIA SUB-RESOURCE ===

    // ➕ [6] Thêm media (URL Cloudinary)
    @PostMapping("/{productId}/media")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<ProductResponse.MediaItem>>> addMedia(
            @PathVariable Long productId,
            @RequestParam List<String> urls,
            @RequestParam(defaultValue = "IMAGE") String type,
            @RequestParam(defaultValue = "false") boolean thumbnail
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                productMediaService.addMedia(productId, urls, type, thumbnail)
        ));
    }

    // 📋 [7] Danh sách media theo product
    @GetMapping("/{productId}/media")
    public ResponseEntity<ApiResponse<List<ProductResponse.MediaItem>>> listMedia(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productMediaService.listMedia(productId)));
    }

    // ❌ [8] Xoá media cụ thể
    @DeleteMapping("/{productId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> removeMedia(
            @PathVariable Long productId,
            @PathVariable Long mediaId
    ) {
        productMediaService.removeMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.msg("Removed"));
    }

    // === 🟨 TAG SUB-RESOURCE ===

    // 🏷️ [9] Lấy tất cả tag có sẵn (để CLB chọn khi tạo product)
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<Tag>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.ok(tagService.getAllTags()));
    }

    // 🏷️ [10] Tìm sản phẩm theo 1 hoặc nhiều tag
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchByTags(
            @RequestParam(required = false) List<String> tags // ví dụ ?tags=event,eco
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.searchByTags(tags)));
    }



}
