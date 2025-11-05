package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.EventStatsResponse;
import com.example.uniclub.dto.response.FraudCaseResponse;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(
        name = "Attendance Management",
        description = """
        Quản lý điểm danh sự kiện:
        - Lấy mã QR động cho từng giai đoạn sự kiện (START / MID / END)
        - Check-in thành viên bằng token JWT QR
        - Quét mã QR cho giai đoạn mới
        - Thống kê sự kiện và phát hiện gian lận
        """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final JwtUtil jwtUtil;

    // ==========================================================
    // 🟢 1. LẤY QR TOKEN MỚI CHO SỰ KIỆN
    // ==========================================================
    @Operation(
            summary = "Lấy QR token động cho sự kiện",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Endpoint được gọi định kỳ (mỗi 30s–60s) để tạo QR động.<br>
                Dùng cho màn hình leader hiển thị QR cho member quét.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "200", description = "Lấy QR thành công"),
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
            }
    )
    @GetMapping("/qr-token/{eventId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQrToken(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "START") String phase
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.getQrTokenForEvent(eventId, phase)
        ));
    }

    // ==========================================================
    // 🙋 2. MEMBER GỬI CHECK-IN
    // ==========================================================
    @Operation(
            summary = "Member check-in bằng QR token",
            description = """
                Dành cho **MEMBER**.<br>
                Thành viên quét mã QR hiển thị trên màn hình leader → gửi token đến endpoint này để điểm danh.<br>
                Token được xác thực bằng JWT từ mã QR và JWT user trong header.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "200", description = "Check-in thành công"),
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "401", description = "Unauthorized hoặc token không hợp lệ")
            }
    )
    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<String>> checkIn(
            @RequestParam("token") String eventJwtToken,
            HttpServletRequest request
    ) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401)
                    .body(ApiResponse.msg("Unauthorized"));

        String jwt = authHeader.substring(7);
        String email = jwtUtil.getSubject(jwt);
        attendanceService.checkInWithToken(eventJwtToken, email);

        return ResponseEntity.ok(ApiResponse.msg("Checked-in successfully"));
    }

    // ==========================================================
    // 📸 3. LEADER SCAN QR GIAI ĐOẠN
    // ==========================================================
    @Operation(
            summary = "Leader quét QR token giai đoạn mới",
            description = """
                Dành cho **CLUB_LEADER** hoặc **VICE_LEADER**.<br>
                Dùng khi leader quét QR token để xác nhận chuyển giai đoạn (START → MID → END).<br>
                Token được sinh tự động từ server.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "200", description = "Scan thành công"),
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> scanQr(
            @RequestParam("token") String qrToken,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.status(401)
                    .body(ApiResponse.msg("Unauthorized"));

        String jwt = authHeader.substring(7);
        String email = jwtUtil.getSubject(jwt);
        attendanceService.scanEventPhase(qrToken, email);

        return ResponseEntity.ok(ApiResponse.msg("✅ Scan success"));
    }

    // ==========================================================
    // 📊 4. THỐNG KÊ SỰ KIỆN
    // ==========================================================
    @Operation(
            summary = "Thống kê điểm danh của sự kiện",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về thông tin tổng quan: số check-in, tỉ lệ tham gia, trạng thái điểm danh từng thành viên.
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "200", description = "Lấy thống kê thành công")
            }
    )
    @GetMapping("/stats/{eventId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<EventStatsResponse>> getStats(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getEventStats(eventId)));
    }

    // ==========================================================
    // 🚨 5. PHÁT HIỆN GIAN LẬN
    // ==========================================================
    @Operation(
            summary = "Lấy danh sách gian lận điểm danh",
            description = """
                Dành cho **CLUB_LEADER**, **VICE_LEADER** hoặc **UNIVERSITY_STAFF**.<br>
                Trả về danh sách các trường hợp nghi ngờ gian lận (check-in trùng, dùng token lạ, v.v.).
                """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.
                            ApiResponse(responseCode = "200", description = "Trả về danh sách gian lận")
            }
    )
    @GetMapping("/fraud/{eventId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<FraudCaseResponse>>> getFraudCases(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getFraudCases(eventId)));
    }
}
