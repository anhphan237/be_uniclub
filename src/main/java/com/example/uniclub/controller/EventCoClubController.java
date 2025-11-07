package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.service.EventCoClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Event Co-Host Management",
        description = """
        Quản lý mối quan hệ đồng tổ chức (Co-Host) giữa các CLB trong sự kiện.<br>
        Bao gồm: cập nhật trạng thái đồng tổ chức (PENDING, APPROVED, REJECTED)
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/event-cohosts")
@RequiredArgsConstructor
public class EventCoClubController {

    private final EventCoClubService eventCoClubService;

    // ==========================================================
    // 🔄 CẬP NHẬT TRẠNG THÁI ĐỒNG TỔ CHỨC
    // ==========================================================
    @Operation(
            summary = "Cập nhật trạng thái đồng tổ chức của CLB trong sự kiện",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER**, hoặc **UNIVERSITY_STAFF**.<br>
                Cho phép cập nhật trạng thái đồng tổ chức của 1 CLB trong sự kiện.<br>
                Các trạng thái bao gồm:
                - `PENDING` → đang chờ duyệt
                - `APPROVED` → đồng ý tham gia
                - `REJECTED` → từ chối đồng tổ chức
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy CLB hoặc sự kiện")
            }
    )
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    @PatchMapping("/{eventId}/{clubId}/status")
    public ResponseEntity<ApiResponse<String>> updateCoHostStatus(
            @PathVariable Long eventId,
            @PathVariable Long clubId,
            @RequestParam EventCoHostStatusEnum status
    ) {
        eventCoClubService.updateStatus(eventId, clubId, status);
        return ResponseEntity.ok(ApiResponse.msg("Cập nhật trạng thái đồng tổ chức thành công: " + status));
    }
}
