package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.CardRequest;
import com.example.uniclub.dto.response.CardResponse;
import com.example.uniclub.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "💳 Club Card Management",
        description = """
        Quản lý thẻ nhận diện (Card) của CLB, bao gồm:
        - Tạo hoặc cập nhật thẻ cho CLB
        - Xem thông tin thẻ theo CLB hoặc ID
        - Xóa thẻ (ADMIN hoặc STAFF)
        - Lấy danh sách toàn bộ thẻ trong hệ thống
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // ==========================================================
    // 🟢 1. TẠO HOẶC CẬP NHẬT CARD CHO CLB
    // ==========================================================
    @Operation(
            summary = "Tạo hoặc cập nhật Card cho CLB",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER**, **ADMIN**, hoặc **UNIVERSITY_STAFF**.<br>
                Nếu CLB chưa có card → tạo mới.<br>
                Nếu đã có → cập nhật nội dung, hình ảnh, QR hoặc mã số card.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo hoặc cập nhật thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
            }
    )
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','ADMIN','UNIVERSITY_STAFF')")
    @PostMapping("/{clubId}")
    public ResponseEntity<ApiResponse<CardResponse>> saveOrUpdate(
            @PathVariable Long clubId,
            @RequestBody CardRequest req
    ) {
        return ResponseEntity.ok(cardService.saveOrUpdate(clubId, req));
    }

    // ==========================================================
    // 🔍 2. LẤY CARD THEO CLUB ID
    // ==========================================================
    @Operation(
            summary = "Xem thông tin Card của CLB",
            description = """
                Dành cho **STUDENT**, **CLUB_LEADER**, **VICE_LEADER**, **UNIVERSITY_STAFF**, hoặc **ADMIN**.<br>
                Lấy thông tin card (logo, QR, mô tả, đường dẫn hình ảnh...) của CLB cụ thể.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thành công")
    )
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF','ADMIN')")
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ApiResponse<CardResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getByClubId(clubId)));
    }

    // ==========================================================
    // 📄 3. LẤY CARD THEO ID
    // ==========================================================
    @Operation(
            summary = "Lấy thông tin Card theo ID",
            description = """
                Public cho tất cả vai trò có quyền xem thẻ của CLB.<br>
                Trả về thông tin chi tiết của card bao gồm id, clubId, hình ảnh và trạng thái.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy card")
            }
    )
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getById(id)));
    }

    // ==========================================================
    // 🗂️ 4. LẤY TOÀN BỘ CARD (ADMIN / STAFF)
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách toàn bộ Card trong hệ thống",
            description = """
                Chỉ dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về danh sách card của tất cả CLB, phục vụ quản trị hệ thống.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    )
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getAll()));
    }

    // ==========================================================
    // 🗑️ 5. XÓA CARD (ADMIN / STAFF)
    // ==========================================================
    @Operation(
            summary = "Xóa Card theo ID",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Xóa (hoặc vô hiệu hóa) thẻ của CLB khỏi hệ thống.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy card")
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.delete(id));
    }
}
