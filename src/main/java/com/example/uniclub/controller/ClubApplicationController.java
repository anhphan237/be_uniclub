package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.ClubApplicationResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubApplicationService;
import com.example.uniclub.service.EmailService;
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
    private final EmailService emailService;

    // ==========================================================
    // 🟢 1. SINH VIÊN NỘP ĐƠN ONLINE
    // ==========================================================
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @Operation(
            summary = "Sinh viên nộp đơn xin lập CLB (kèm OTP)",
            description = """
            Dành cho **STUDENT**.<br>
            Sinh viên nhập mã OTP được UniStaff cấp qua email để gửi đơn xin lập CLB.<br>
            Nếu OTP hợp lệ → lưu đơn với trạng thái `PENDING`.
            """
    )
    public ResponseEntity<ApiResponse<ClubApplicationResponse>> createOnline(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ClubApplicationCreateRequest req,
            @RequestParam String otp
    ) {
        clubApplicationService.verifyOtp(user.getUsername(), otp);

        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.createOnline(user.getUserId(), req)
        ));
    }

    // ==========================================================
    // 🧑‍💼 2. GỬI OTP CHO SINH VIÊN
    // ==========================================================
    @Operation(
            summary = "Gửi mã OTP cho sinh viên xin lập CLB",
            description = """
            Dành cho **UNIVERSITY_STAFF**.<br>
            Gửi mã OTP qua email cho sinh viên để họ có thể nộp đơn xin lập CLB.<br>
            Mã OTP có hiệu lực trong 48 giờ.
            """
    )
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<String>> sendOtpToStudent(@RequestParam String studentEmail) {
        var student = clubApplicationService.findStudentByEmail(studentEmail);

        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        clubApplicationService.saveOtp(studentEmail, otp);

        String html = String.format("""
        <p>Hello <b>%s</b>,</p>
        <p>You have been granted permission to submit a request to establish a new club on the <b>UniClub</b> system.</p>
        <p>Your OTP code is:</p>
        <div style="font-size: 26px; color: #ff6600; font-weight: bold;">%s</div>
        <p>This code is valid for <b>48 hours</b>. Please do not share it with anyone else.</p>
        """, student.getFullName(), otp);

        emailService.sendEmail(studentEmail, "[UniClub] OTP code for Club Creation Request", html);

        return ResponseEntity.ok(ApiResponse.msg("OTP has been sent to " + studentEmail));
    }

    // ==========================================================
    // 🟠 3. PHÊ DUYỆT / TỪ CHỐI ĐƠN
    // ==========================================================
    @Operation(
            summary = "Phê duyệt hoặc từ chối đơn ứng tuyển",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Cho phép duyệt đơn hoặc từ chối đơn với lý do cụ thể.<br>
                Nếu phê duyệt → chuyển trạng thái `APPROVED`.
                """
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
    // 🟢 4. TẠO TÀI KHOẢN LEADER & VICE LEADER
    // ==========================================================
    @Operation(
            summary = "Tạo tài khoản CLB sau khi phê duyệt",
            description = """
                Dành cho **UNIVERSITY_STAFF**.<br>
                Sau khi đơn được phê duyệt, UniStaff tạo tài khoản Leader và Vice Leader.
                """
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
    // 🟣 5. SINH VIÊN XEM DANH SÁCH ĐƠN CỦA MÌNH
    // ==========================================================
    @Operation(
            summary = "Sinh viên xem các đơn mình đã nộp",
            description = "Dành cho STUDENT."
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
    // 🔵 6. XEM CHI TIẾT 1 ĐƠN
    // ==========================================================
    @Operation(
            summary = "Xem chi tiết đơn ứng tuyển",
            description = "Dành cho STUDENT, UNIVERSITY_STAFF hoặc ADMIN."
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
    // ⚪ 7. LẤY TOÀN BỘ ĐƠN
    // ==========================================================
    @Operation(summary = "Lấy toàn bộ đơn", description = "Dành cho ADMIN hoặc UNIVERSITY_STAFF.")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getAllApplications() {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getAllApplications()
        ));
    }

    // ==========================================================
    // 🟤 8. DANH SÁCH ĐƠN PENDING
    // ==========================================================
    @Operation(summary = "Danh sách đơn chờ duyệt", description = "Dành cho STAFF hoặc ADMIN.")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getPending()
        ));
    }

    // ==========================================================
    // 🟣 9. THỐNG KÊ
    // ==========================================================
    @Operation(summary = "Thống kê số lượng đơn")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.getStatistics()
        ));
    }

    // ==========================================================
    // 🔵 10. TÌM KIẾM ĐƠN
    // ==========================================================
    @Operation(summary = "Tìm kiếm đơn ứng tuyển")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ClubApplicationResponse>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(
                clubApplicationService.search(keyword)
        ));
    }
}
