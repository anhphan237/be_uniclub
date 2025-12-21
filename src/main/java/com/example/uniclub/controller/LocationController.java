package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.LocationCreateRequest;
import com.example.uniclub.dto.request.LocationUpdateRequest;
import com.example.uniclub.dto.response.LocationResponse;
import com.example.uniclub.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Tag(
        name = "Location Management",
        description = """
        Quản lý địa điểm tổ chức sự kiện trong hệ thống UniClub:<br>
        - Thêm, xem, xoá và phân trang danh sách địa điểm.<br>
        - Địa điểm có thể được gán cho các sự kiện hoặc CLB tổ chức.<br>
        Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // =========================================================
    // 🟢 1. CREATE
    // =========================================================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Tạo mới địa điểm tổ chức",
            description = """
                Dành cho ADMIN hoặc UNIVERSITY_STAFF.<br>
                Nhập tên địa điểm và thông tin liên quan để thêm mới.
                """
    )
    public ResponseEntity<LocationResponse> create(@RequestBody @Valid LocationCreateRequest req) {
        return ResponseEntity.ok(locationService.create(req));
    }

    // =========================================================
// 🔍 2. GET BY ID
// =========================================================
    @GetMapping("/{id}")
    @Operation(
            summary = "Xem chi tiết địa điểm",
            description = "Lấy thông tin chi tiết của một địa điểm. (Không yêu cầu quyền)"
    )
    public ResponseEntity<LocationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.get(id));
    }

    // =========================================================
// 📋 3. LIST
// =========================================================
    @GetMapping
    @Operation(
            summary = "Lấy danh sách địa điểm (phân trang)",
            description = "Trả về danh sách tất cả địa điểm. (Không yêu cầu quyền)"
    )
    public ResponseEntity<?> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(locationService.list(pageable));
    }


    // =========================================================
    // 🗑️ 4. DELETE
    // =========================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Xoá địa điểm",
            description = """
                Chỉ ADMIN và UNIVERSITY_STAFF có quyền xoá.<br>
                Chỉ xoá nếu địa điểm chưa được gán cho sự kiện nào.
                """
    )
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // ✏️ 5. UPDATE
    // =========================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Cập nhật địa điểm",
            description = "ADMIN và UNIVERSITY_STAFF có thể sửa tên, địa chỉ, sức chứa."
    )
    public ResponseEntity<ApiResponse<LocationResponse>> update(
            @PathVariable Long id,
            @RequestBody LocationUpdateRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.update(id, req)));
    }



    @GetMapping("/{id}/conflicts")
    @Operation(summary = "Kiểm tra trùng lịch tại địa điểm")
    public ResponseEntity<?> checkConflicts(
            @PathVariable("id") Long locationId,   // ✅ FIX: dùng PathVariable

            @RequestParam
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            @Parameter(
                    description = "Ngày (dd-MM-yyyy)",
                    example = "20-12-2025"
            )
            LocalDate date,

            @RequestParam
            @DateTimeFormat(pattern = "HH:mm")
            @Parameter(
                    description = "Giờ bắt đầu (HH:mm)",
                    example = "09:00",
                    schema = @Schema(type = "string", format = "time")
            )
            LocalTime startTime,

            @RequestParam
            @DateTimeFormat(pattern = "HH:mm")
            @Parameter(
                    description = "Giờ kết thúc (HH:mm)",
                    example = "11:00",
                    schema = @Schema(type = "string", format = "time")
            )
            LocalTime endTime
    ) {
        return ResponseEntity.ok(
                locationService.checkConflict(locationId, date, startTime, endTime)
        );
    }






}
