package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(
        name = "Club Application Management",
        description = """
        Quản lý đơn ứng tuyển thành lập CLB tại trường:
        - Sinh viên nộp đơn thành lập CLB
        - UniStaff phê duyệt / từ chối / tạo tài khoản CLB
        - Xem danh sách đơn, tìm kiếm và thống kê trạng thái
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/club-applications")
@RequiredArgsConstructor
public class ClubApplicationController {

    private final ClubApplicationService clubApplicationService;

    // ==========================================================
    // 🟢 1. SINH VIÊN NỘP ĐƠN ONLINE
    // ==========================================================
    @Operation(
            summary = "Sinh viên nộp đơn online",
            description = """
                Dành cho **STUDENT**.<br>
                Sinh viên nhập thông tin đề xuất thành lập CLB (tên, mô tả, mục tiêu, dự kiến hoạt động...).<br>
                Hệ thống tự động gán trạng thái ban đầu là `PENDING`.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Nộp đơn thành công")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> createOnline(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ClubApplicationCreateRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.createOnline(user.getUserId(), req)
        ));
    }

    // ==========================================================
    // 🟠 2. PHÊ DUYỆT / TỪ CHỐI ĐƠN (UniStaff)
    // ==========================================================
    @Operation(
            summary = "Phê duyệt hoặc từ chối đơn ứng tuyển",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Cho phép nhân viên nhà trường duyệt đơn hoặc từ chối với lý do cụ thể.<br>
                Nếu phê duyệt → chuyển trạng thái thành `APPROVED` và cho phép tạo tài khoản CLB.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Phê duyệt / từ chối thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn")
            }
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> approveClubApplication(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails staff,
            @Valid @RequestBody ClubApplicationDecisionRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.decide(id, staff.getUserId(), req)
        ));
    }

    // ==========================================================
    // 🟢 3. TẠO TÀI KHOẢN LEADER & VICE LEADER (UniStaff)
    // ==========================================================
    @Operation(
            summary = "Tạo tài khoản CLB sau khi phê duyệt",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Sau khi đơn được phê duyệt, UniStaff có thể tạo tài khoản **CLUB_LEADER** và **VICE_LEADER**.<br>
                Tự động gửi email thông báo cho các tài khoản vừa được tạo.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Tạo tài khoản CLB thành công")
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping("/create-club-accounts")
    public ResponseEntity<ApiResponse<String>> createClubAccounts(
            @Valid @RequestBody CreateClubAccountsRequest request
    ) {
        clubApplicationService.createClubAccounts(request);
        return ResponseEntity.ok(ApiResponse.ok("Club accounts created successfully."));
    }

    // ==========================================================
    // 🟣 4. SINH VIÊN XEM ĐƠN CỦA MÌNH
    // ==========================================================
    @Operation(
            summary = "Sinh viên xem danh sách đơn của mình",
            description = """
                Dành cho **STUDENT**.<br>
                Hiển thị danh sách các đơn mà sinh viên hiện tại đã nộp (PENDING / APPROVED / REJECTED).
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    )
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getByUser(user.getUserId())
        ));
    }

    // ==========================================================
    // 🔵 5. XEM CHI TIẾT 1 ĐƠN
    // ==========================================================
    @Operation(
            summary = "Xem chi tiết đơn ứng tuyển",
            description = """
                Dành cho **ADMIN**, **UNIVERSITY_STAFF**, hoặc **STUDENT**.<br>
                Hiển thị chi tiết đơn bao gồm trạng thái, người nộp, và nội dung.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy đơn")
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','STUDENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> getById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getById(user.getUserId(), user.getRoleName(), id)
        ));
    }

    // ==========================================================
    // ⚪ 6. LẤY TOÀN BỘ ĐƠN
    // ==========================================================
    @Operation(
            summary = "Lấy toàn bộ đơn ứng tuyển CLB",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về toàn bộ danh sách đơn ứng tuyển hiện có.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getAllApplications()
        ));
    }

    // ==========================================================
    // 🟤 7. DANH SÁCH ĐƠN CHỜ DUYỆT
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách đơn đang chờ phê duyệt",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về danh sách các đơn có trạng thái `PENDING`.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getPending()
        ));
    }

    // ==========================================================
    // 🟣 8. THỐNG KÊ SỐ LƯỢNG ĐƠN THEO TRẠNG THÁI
    // ==========================================================
    @Operation(
            summary = "Thống kê số lượng đơn theo trạng thái",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Thống kê số lượng đơn theo từng trạng thái: `PENDING`, `APPROVED`, `REJECTED`.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Lấy thống kê thành công")
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getStatistics()
        ));
    }

    // ==========================================================
    // 🔵 9. TÌM KIẾM ĐƠN THEO TỪ KHÓA
    // ==========================================================
    @Operation(
            summary = "Tìm kiếm đơn ứng tuyển theo từ khóa",
            description = """
                Dành cho **ADMIN** hoặc **UNIVERSITY_STAFF**.<br>
                Cho phép tìm theo tên CLB, người nộp, hoặc trạng thái đơn.
                """,
            responses = @io.swagger.v3.oas.annotations.responses.
                    ApiResponse(responseCode = "200", description = "Tìm kiếm thành công")
    )
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> search(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.search(keyword)
        ));
    }
}
