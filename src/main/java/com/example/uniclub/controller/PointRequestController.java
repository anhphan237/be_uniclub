package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.PointRequestCreateRequest;
import com.example.uniclub.dto.response.PointRequestResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.PointRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Point Request Management",
        description = """
        Quản lý **yêu cầu xin điểm (Point Requests)** giữa các CLB và Ban đại học trong hệ thống UniClub:<br>
        - CLB có thể gửi yêu cầu cấp điểm để tổ chức sự kiện hoặc hoạt động.<br>
        - University Staff xét duyệt, từ chối hoặc xem lịch sử yêu cầu.<br>
        - Hỗ trợ cả dạng phân trang và toàn bộ danh sách.<br>
        Dành cho vai trò: **CLUB_LEADER**, **VICE_LEADER**, **UNIVERSITY_STAFF**.
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/point-requests")
@RequiredArgsConstructor
public class PointRequestController {

    private final PointRequestService pointRequestService;

    // ============================================================
    // 🟢 1️⃣ CLUB GỬI YÊU CẦU XIN ĐIỂM
    // ============================================================
    @Operation(
            summary = "CLB tạo yêu cầu xin điểm từ Ban đại học",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Gửi yêu cầu cấp điểm để chuẩn bị ngân sách tổ chức sự kiện hoặc hoạt động CLB.<br>
                Trạng thái ban đầu: `PENDING`.
                """
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<PointRequestResponse>> createRequest(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody PointRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.createRequest(principal, req)));
    }

    // ============================================================
    // 🟠 2️⃣ UNIVERSITY STAFF - XEM CÁC YÊU CẦU ĐANG CHỜ DUYỆT
    // ============================================================
    @Operation(
            summary = "University Staff xem danh sách yêu cầu đang chờ duyệt",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về danh sách các yêu cầu điểm có trạng thái `PENDING`.<br>
                Dùng cho giao diện dashboard duyệt điểm.
                """
    )
    @GetMapping("/pending")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<PointRequestResponse>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.getPendingRequests()));
    }

    // ============================================================
    // 🟢 3️⃣ UNIVERSITY STAFF - DUYỆT HOẶC TỪ CHỐI YÊU CẦU
    // ============================================================
    @Operation(
            summary = "University Staff duyệt hoặc từ chối yêu cầu xin điểm",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Chuyển trạng thái của yêu cầu thành `APPROVED` hoặc `REJECTED` tùy theo lựa chọn.<br>
                Khi được duyệt, điểm sẽ được chuyển đến ví CLB tương ứng.
                """
    )
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> reviewRequest(
            @PathVariable Long id,
            @RequestParam boolean approve) {
        return ResponseEntity.ok(ApiResponse.msg(pointRequestService.reviewRequest(id, approve, null)));
    }

    // ============================================================
    // 🟣 4️⃣ UNIVERSITY STAFF - XEM TẤT CẢ YÊU CẦU (PHÂN TRANG)
    // ============================================================
    @Operation(
            summary = "Lấy danh sách yêu cầu điểm (phân trang)",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về danh sách các yêu cầu cấp điểm kèm thông tin CLB, ngày gửi, và trạng thái duyệt.<br>
                Hỗ trợ phân trang để hiển thị hiệu quả trên giao diện quản trị.
                """
    )
    @GetMapping
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<Page<PointRequestResponse>> getAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(pointRequestService.list(pageable));
    }

    // ============================================================
    // 🔵 5️⃣ UNIVERSITY STAFF - XEM CHI TIẾT MỘT YÊU CẦU
    // ============================================================
    @Operation(
            summary = "Lấy chi tiết một yêu cầu xin điểm theo ID",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về thông tin chi tiết: CLB gửi yêu cầu, số điểm xin, mô tả và trạng thái hiện tại.
                """
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<PointRequestResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.get(id)));
    }

    // ============================================================
    // 🟤 6️⃣ UNIVERSITY STAFF - XEM TOÀN BỘ YÊU CẦU (KHÔNG PHÂN TRANG)
    // ============================================================
    @Operation(
            summary = "Lấy toàn bộ danh sách yêu cầu điểm (không phân trang)",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Trả về danh sách tất cả yêu cầu cấp điểm (bao gồm cả đã duyệt, từ chối, chờ duyệt).
                """
    )
    @GetMapping("/all")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<PointRequestResponse>>> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.ok(pointRequestService.getAllRequests()));
    }
}
