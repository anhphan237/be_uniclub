package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.request.ProductUpdateRequest;
import com.example.uniclub.dto.response.ProductMediaResponse;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.ProductStockHistory;
import com.example.uniclub.service.ProductMediaService;
import com.example.uniclub.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/clubs/{clubId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMediaService productMediaService;

    // [1] Tạo sản phẩm
    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @PathVariable Long clubId,
            @Valid @RequestBody ProductCreateRequest req
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(productService.create(req, clubId)));
        } catch (com.example.uniclub.exception.ApiException ex) {
            if (ex.getStatus() != null && ex.getStatus().value() == 200 && ex.getMessage() != null && ex.getMessage().startsWith("REACTIVATED:")) {
                Long id = Long.valueOf(ex.getMessage().substring("REACTIVATED:".length()));
                return ResponseEntity.ok(ApiResponse.ok(productService.get(id)));
            }
            throw ex;
        }
    }

    // [2] Lấy chi tiết sản phẩm
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.get(id)));
    }

    // [3] List theo CLB (non-paged)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listByClub(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.listByClub(clubId, includeInactive, includeArchived)
        ));
    }

    // [4] List tất cả cho ADMIN / STAFF (có filter)
    @GetMapping("/_all")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> listAll(
            Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.adminFilterList(pageable, status, type, tag, keyword)
        ));
    }

    // [5] Cập nhật thông tin
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.update(id, req)));
    }

    // [6] Cập nhật tồn kho
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer delta,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateStock(id, delta, note)));
    }

    // [7] Lịch sử tồn kho
    @GetMapping("/{id}/stock-history")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<ProductStockHistory>>> getStockHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getStockHistory(id)));
    }

    // [8] Xóa (soft delete)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Product set to INACTIVE"));
    }

    // [9] Search theo tag
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchByTags(@RequestParam(required = false) List<String> tags) {
        return ResponseEntity.ok(ApiResponse.ok(productService.searchByTags(tags)));
    }


    // === MEDIA (UPLOAD / LIST / DELETE) ===

    @PostMapping(
            value = "/{productId}/media",
            consumes = "multipart/form-data"
    )
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductMediaResponse>> uploadProductMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        ProductMediaResponse res = productMediaService.uploadMedia(productId, file);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }


    @GetMapping("/{productId}/media")
    public ResponseEntity<ApiResponse<List<ProductMediaResponse>>> getProductMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId
    ) {
        // ✅ Lấy danh sách ảnh/video của product
        return ResponseEntity.ok(ApiResponse.ok(productMediaService.listMedia(productId)));
    }

    @DeleteMapping("/{productId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> removeProductMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @PathVariable Long mediaId
    ) {
        // ✅ Xóa 1 media khỏi sản phẩm
        productMediaService.removeMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.msg("Removed successfully"));
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<?>> updateProduct(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                productService.updateProduct(clubId, productId, req)
        ));
    }


}
