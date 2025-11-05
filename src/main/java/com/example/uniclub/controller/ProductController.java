package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ProductCreateRequest;
import com.example.uniclub.dto.request.ProductUpdateRequest;
import com.example.uniclub.dto.response.ProductMediaResponse;
import com.example.uniclub.dto.response.ProductResponse;
import com.example.uniclub.entity.ProductStockHistory;
import com.example.uniclub.service.ProductMediaService;
import com.example.uniclub.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(
        name = " Product Management (CLUB)",
        description = """
        Quản lý sản phẩm & media của CLB bao gồm:
        - Tạo / Cập nhật / Xóa sản phẩm
        - Upload ảnh, cập nhật ảnh, reorder và đặt thumbnail
        - Quản lý tồn kho, lịch sử điểm và trạng thái sản phẩm
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/clubs/{clubId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMediaService productMediaService;

    // ==========================================================
    // 🧩 1. TẠO SẢN PHẨM
    // ==========================================================
    @Operation(
            summary = "Tạo sản phẩm mới cho CLB",
            description = """
                Dành cho vai trò **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Tạo sản phẩm loại `CLUB_ITEM` hoặc `EVENT_ITEM`.<br>
                Nếu là `EVENT_ITEM`, cần có `eventId` hợp lệ.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo sản phẩm thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
            }
    )
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

    // ==========================================================
    // 🔍 2. XEM CHI TIẾT SẢN PHẨM
    // ==========================================================
    @Operation(
            summary = "Xem chi tiết sản phẩm",
            description = "Hiển thị thông tin chi tiết của sản phẩm, bao gồm tên, điểm đổi, số lượng, ảnh và trạng thái.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy dữ liệu thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.get(id)));
    }

    // ==========================================================
    // 📦 3. DANH SÁCH SẢN PHẨM CỦA CLB
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách sản phẩm theo CLB",
            description = """
                Trả về danh sách toàn bộ sản phẩm của CLB.<br>
                Có thể lọc theo trạng thái hoạt động hoặc lưu trữ.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trả về danh sách sản phẩm")
    )
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

    // ==========================================================
    // 🧑‍💼 4. ADMIN / STAFF LỌC TẤT CẢ SẢN PHẨM
    // ==========================================================
    @Operation(
            summary = "Lọc sản phẩm (ADMIN / STAFF)",
            description = """
                Chỉ dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Có thể lọc theo trạng thái, loại, tag hoặc từ khóa.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Danh sách lọc thành công")
    )
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

    // ==========================================================
    // ✏️ 5. CẬP NHẬT SẢN PHẨM
    // ==========================================================
    @Operation(
            summary = "Cập nhật thông tin sản phẩm",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Cập nhật thông tin cơ bản của sản phẩm như tên, mô tả, điểm, số lượng, loại, hoặc trạng thái.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.update(id, req)));
    }

    // ==========================================================
    // ⚖️ 6. CẬP NHẬT TỒN KHO
    // ==========================================================
    @Operation(
            summary = "Cập nhật số lượng tồn kho",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Tăng hoặc giảm số lượng tồn kho sản phẩm, có thể thêm ghi chú.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật tồn kho thành công")
    )
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer delta,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateStock(id, delta, note)));
    }

    // ==========================================================
    // 📜 7. LỊCH SỬ TỒN KHO
    // ==========================================================
    @Operation(
            summary = "Lấy lịch sử thay đổi tồn kho",
            description = "Hiển thị lịch sử thay đổi tồn kho của sản phẩm (Leader, Vice Leader, University Staff có quyền xem).",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trả về danh sách lịch sử")
    )
    @GetMapping("/{id}/stock-history")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<ProductStockHistory>>> getStockHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getStockHistory(id)));
    }

    // ==========================================================
    // 🗑️ 8. XÓA SẢN PHẨM
    // ==========================================================
    @Operation(
            summary = "Xóa (vô hiệu hóa) sản phẩm",
            description = "Soft delete sản phẩm, chỉ **CLUB_LEADER** hoặc **VICE_LEADER** có quyền thực hiện.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sản phẩm đã chuyển sang trạng thái INACTIVE")
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Product set to INACTIVE"));
    }

    // ==========================================================
    // 🔍 9. TÌM KIẾM SẢN PHẨM THEO TAG
    // ==========================================================
    @Operation(
            summary = "Tìm kiếm sản phẩm theo tag",
            description = "Tìm các sản phẩm có chứa các tag nhất định (public API, không yêu cầu quyền).",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trả về danh sách sản phẩm phù hợp")
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchByTags(@RequestParam(required = false) List<String> tags) {
        return ResponseEntity.ok(ApiResponse.ok(productService.searchByTags(tags)));
    }

    // ==========================================================
    // 📸 10. UPLOAD / LIST / DELETE MEDIA
    // ==========================================================
    @Operation(
            summary = "Upload 1 ảnh hoặc video sản phẩm",
            description = "Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**. File sẽ được lưu trên Cloudinary theo `productId`.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload thành công")
    )
    @PostMapping(value = "/{productId}/media", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<ProductMediaResponse>> uploadProductMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        ProductMediaResponse res = productMediaService.uploadMedia(productId, file);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    @Operation(
            summary = "Lấy danh sách ảnh/video của sản phẩm",
            description = "Public API, trả về toàn bộ media thuộc sản phẩm (đã sắp xếp theo displayOrder)."
    )
    @GetMapping("/{productId}/media")
    public ResponseEntity<ApiResponse<List<ProductMediaResponse>>> getProductMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(productMediaService.listMedia(productId)));
    }

    @Operation(
            summary = "Xóa ảnh sản phẩm",
            description = "Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**. Xóa file media khỏi DB và Cloudinary.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công")
    )
    @DeleteMapping("/{productId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> removeProductMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @PathVariable Long mediaId
    ) {
        productMediaService.removeMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.msg("Removed successfully"));
    }

    // ==========================================================
    // ✏️ 11. UPDATE MEDIA
    // ==========================================================
    @Operation(
            summary = "Cập nhật ảnh/video sản phẩm",
            description = "Dành cho **CLUB_LEADER**. Có thể thay file hoặc chỉnh sửa metadata như thumbnail, type, displayOrder.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật media thành công")
    )
    @PutMapping("/{productId}/media/{mediaId}")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ProductMediaResponse> updateMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @PathVariable Long mediaId,
            @ModelAttribute com.example.uniclub.dto.request.ProductMediaUpdateRequest req
    ) throws IOException {
        return ResponseEntity.ok(productMediaService.updateMedia(productId, mediaId, req));
    }

    // ==========================================================
    // 📂 12. BULK UPLOAD MEDIA
    // ==========================================================
    @Operation(
            summary = "Upload nhiều ảnh/video cùng lúc",
            description = "Dành cho **CLUB_LEADER**. Upload nhiều file trong 1 request (tối đa 5-10 ảnh).",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload thành công")
    )
    @PostMapping(value = "/{productId}/media/bulk", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<List<ProductMediaResponse>>> uploadMultiple(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {
        return ResponseEntity.ok(ApiResponse.ok(productMediaService.uploadMultiple(productId, files)));
    }

    // ==========================================================
    // 🔃 13. REORDER MEDIA
    // ==========================================================
    @Operation(
            summary = "Thay đổi thứ tự hiển thị media",
            description = "Dành cho **CLUB_LEADER**. Truyền danh sách `mediaId` theo thứ tự mong muốn để cập nhật `displayOrder`.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reorder thành công")
    )
    @PutMapping("/{productId}/media/reorder")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> reorderMedia(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @RequestBody com.example.uniclub.dto.request.MediaReorderRequest req
    ) {
        productMediaService.reorder(productId, req);
        return ResponseEntity.ok(ApiResponse.msg("Reordered successfully"));
    }

    // ==========================================================
    // 🌟 14. SET THUMBNAIL
    // ==========================================================
    @Operation(
            summary = "Đặt ảnh thumbnail chính",
            description = "Dành cho **CLUB_LEADER**. Chọn 1 ảnh làm thumbnail chính, các ảnh khác tự động bỏ cờ thumbnail.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thumbnail thành công")
    )
    @PutMapping("/{productId}/media/{mediaId}/thumbnail")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> setThumbnail(
            @PathVariable Long clubId,
            @PathVariable Long productId,
            @PathVariable Long mediaId
    ) {
        productMediaService.setThumbnail(productId, mediaId);
        return ResponseEntity.ok(ApiResponse.msg("Thumbnail updated successfully"));
    }


    // ==========================================================
// ⚙️ PATCH: CẬP NHẬT NHANH SẢN PHẨM
// ==========================================================
    @Operation(
            summary = "Cập nhật nhanh thông tin sản phẩm (PATCH)",
            description = """
        Dành cho **CLUB_LEADER**.<br>
        Cho phép chỉnh sửa một phần thông tin sản phẩm 
        (ví dụ: tên, mô tả, điểm, trạng thái...).<br>
        Thường dùng khi chỉ cần sửa 1-2 trường mà không cần gửi lại toàn bộ dữ liệu sản phẩm.
        """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Cập nhật sản phẩm thành công"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Dữ liệu không hợp lệ"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Không tìm thấy sản phẩm"
                    )
            }
    )
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
