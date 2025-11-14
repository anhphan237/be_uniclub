package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.LocationCreateRequest;
import com.example.uniclub.dto.request.LocationUpdateRequest;
import com.example.uniclub.dto.response.LocationResponse;
import com.example.uniclub.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Operation(
            summary = "Tạo mới địa điểm tổ chức",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Nhập tên địa điểm, mô tả và thông tin liên quan để thêm mới.<br>
                Trả về đối tượng địa điểm vừa được tạo.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tạo địa điểm thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<LocationResponse> create(@RequestBody @Valid LocationCreateRequest req) {
        return ResponseEntity.ok(locationService.create(req));
    }

    // =========================================================
    // 🔍 2. GET BY ID
    // =========================================================
    @Operation(
            summary = "Xem chi tiết địa điểm theo ID",
            description = """
                Trả về thông tin chi tiết của một địa điểm cụ thể.<br>
                Dùng cho các trang chi tiết hoặc chọn địa điểm khi tạo sự kiện.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin địa điểm thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy địa điểm")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.get(id));
    }

    // =========================================================
    // 📋 3. LIST & PAGINATION
    // =========================================================
    @Operation(
            summary = "Lấy danh sách địa điểm (phân trang)",
            description = """
                Trả về danh sách tất cả địa điểm có trong hệ thống.<br>
                Hỗ trợ phân trang, sắp xếp, tìm kiếm để hiển thị trong trang quản lý.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách địa điểm thành công")
            }
    )
    @GetMapping
    public ResponseEntity<?> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(locationService.list(pageable));
    }

    // =========================================================
    // 🗑️ 4. DELETE
    // =========================================================
    @Operation(
            summary = "Xoá địa điểm theo ID",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Chỉ có thể xoá nếu địa điểm chưa được gán cho sự kiện nào.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Xoá địa điểm thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền xoá địa điểm"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy địa điểm")
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'UNIVERSITY_STAFF')")
    @Operation(
            summary = "Cập nhật địa điểm tổ chức",
            description = """
                Thay đổi tên, địa chỉ hoặc sức chứa của địa điểm.<br>
                Chỉ ADMIN và UNIVERSITY_STAFF được phép thực hiện.
                """
    )
    public ResponseEntity<ApiResponse<LocationResponse>> update(
            @PathVariable Long id,
            @RequestBody LocationUpdateRequest req
    ) {
        return ResponseEntity.ok(
                ApiResponse.ok(locationService.update(id, req))
        );
    }

}
