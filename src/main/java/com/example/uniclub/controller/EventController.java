package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(
        name = "Event Management",
        description = """
    Quản lý toàn bộ vòng đời của sự kiện trong hệ thống UniClub:
    - Tạo, cập nhật, lọc và xoá sự kiện.
    - Quản lý tham gia của sinh viên (đăng ký, huỷ, điểm danh).
    - Xác nhận đồng tổ chức (Co-host) và quản lý nhân sự sự kiện.
    - Theo dõi ví sự kiện, kết toán (settlement) và hoàn điểm.
    - Quản lý phản hồi (feedback) sau sự kiện cho cả CLB và sinh viên.
    - Dành cho các vai trò: **ADMIN**, **UNIVERSITY_STAFF**, **CLUB_LEADER**, **VICE_LEADER**, **STUDENT**.
    """
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {


    private final EventService eventService;
    private final EventPointsService eventPointsService;
    private final EventStaffService eventStaffService;
    private final EventWalletService eventWalletService;
    private final AttendanceService attendanceService;
    private final EventFeedbackService eventFeedbackService;
    // =========================================================
    // 🔹 1. CRUD
    // =========================================================
    @Operation(
            summary = "Tạo mới sự kiện",
            description = """
                Dành cho **ADMIN** hoặc **CLUB_LEADER**.<br>
                Nhập thông tin cơ bản, ngân sách, thời gian, loại và CLB tổ chức.<br>
                Trả về đối tượng sự kiện vừa tạo.
                """)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.create(req)));
    }
    @Operation(summary = "Lấy chi tiết sự kiện theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.get(id)));
    }
    @Operation(summary = "Phân trang danh sách sự kiện")
    @GetMapping
    public ResponseEntity<Page<EventResponse>> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.list(pageable));
    }
    @Operation(summary = "Lấy tất cả sự kiện (Admin/Staff/Leader)")
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(
                eventService.getAllEvents()
        );
    }
    @Operation(summary = "Xoá sự kiện theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // =========================================================
    // 🔹 2. PARTICIPATION
    // =========================================================
    @Operation(
            summary = "Sinh viên đăng ký tham gia sự kiện",
            description = """
                Dành cho **STUDENT**.<br>
                Khi đăng ký, hệ thống trừ điểm cam kết (commit points) từ ví sinh viên.<br>
                Nếu huỷ đúng hạn, điểm sẽ hoàn lại.
                """)
    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> register(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventRegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.register(principal, req)));
    }
    @Operation(summary = "Sinh viên điểm danh sự kiện (check-in)")
    @PostMapping("/checkin")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> checkin(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventCheckinRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.checkin(principal, req)));
    }
    @Operation(summary = "Sinh viên huỷ đăng ký sự kiện")
    @PutMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> cancelRegistration(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.cancelRegistration(principal, eventId)));
    }
    @Operation(
            summary = "Hoàn thành sự kiện (Leader/Staff xác nhận)",
            description = """
        Khi Leader hoặc University Staff xác nhận sự kiện đã kết thúc:
        - Hệ thống sẽ tự động **settle điểm thưởng và hoàn điểm cam kết** cho thành viên.
        - **Điểm dư** trong ví sự kiện sẽ được hoàn lại cho CLB chủ trì và các CLB đồng tổ chức.
        - Gửi thông báo hoàn tất tới các bên liên quan.
        """)
    @PostMapping("/{eventId}/complete")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> completeEvent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId
    ) {
        // ✅ Gọi service trung tâm đã chuẩn hóa logic finish
        String msg = eventService.finishEvent(eventId, principal);

        return ResponseEntity.ok(ApiResponse.msg(msg));
    }



    // =========================================================
    // 🔹 3. LOOKUP
    // =========================================================
    @Operation(summary = "Lấy danh sách sự kiện của một CLB")
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<EventResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getByClubId(clubId));
    }

    @Operation(summary = "Lấy danh sách sự kiện đồng tổ chức của CLB")
    @GetMapping("/club/{clubId}/cohost")
    public ResponseEntity<List<EventResponse>> getCoHostedEvents(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getCoHostedEvents(clubId));
    }
    @Operation(summary = "Tra cứu sự kiện theo mã check-in code")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<EventResponse>> getByCheckInCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCheckInCode(code)));
    }
    @Operation(summary = "Lọc sự kiện theo tên, ngày hoặc trạng thái")
    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> filter(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) EventStatusEnum status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.filter(name, date, status, pageable));
    }
    @Operation(summary = "Lấy sự kiện sắp diễn ra")
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<?>> getUpcomingEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getUpcomingEvents()));
    }
    @Operation(summary = "Lấy sự kiện đang diễn ra")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getActiveEvents()));
    }
    @Operation(summary = "Lấy danh sách sự kiện mà sinh viên đã tham gia")
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getMyEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getMyEvents(principal)));
    }

    // =========================================================
    // 🔹 4. CO-HOST CONFIRMATION
    // =========================================================
    @Operation(summary = "Phản hồi lời mời đồng tổ chức (Co-host)")
    @PostMapping("/{eventId}/cohost/respond")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> respondCohost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId,
            @RequestParam boolean accept) {
        String msg = eventService.respondCoHost(eventId, principal, accept);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }



    // =========================================================
    // 🔹 5. STAFF MANAGEMENT
    // =========================================================
    @Operation(summary = "Gán nhân sự (staff) cho sự kiện")
    @PostMapping("/{id}/staffs")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<EventStaffResponse>> assignStaff(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @RequestParam Long membershipId,
            @RequestParam(required = false) String duty) {
        return ResponseEntity.ok(ApiResponse.ok(eventStaffService.assignStaff(id, membershipId, duty)));
    }
    @Operation(summary = "Huỷ phân công nhân sự sự kiện")
    @DeleteMapping("/{id}/staffs/{staffId}")
    public ResponseEntity<ApiResponse<String>> unassignStaff(
            @PathVariable Long id, @PathVariable Long staffId) {
        eventStaffService.unassignStaff(staffId);
        return ResponseEntity.ok(ApiResponse.msg("Staff unassigned successfully"));
    }
    @Operation(summary = "Lấy danh sách nhân sự được phân công cho sự kiện")
    @GetMapping("/{id}/staffs")
    public ResponseEntity<ApiResponse<List<EventStaffResponse>>> getEventStaffs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventStaffList(id)));
    }


    @Operation(summary = "Xem chi tiết ví sự kiện")
    @GetMapping("/{eventId}/wallet/detail")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER','ADMIN')")
    public ResponseEntity<ApiResponse<EventWalletResponse>> getEventWalletDetail(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventWalletService.getEventWalletDetail(eventId)));
    }

    // =========================================================
    // 🔹 7. ATTENDANCE
    // =========================================================
    @Operation(summary = "Lấy QR token cho điểm danh sự kiện")
    @GetMapping("/{eventId}/attendance/qr")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceQr(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "START") String phase) {

        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getQrTokenForEvent(eventId, phase)));
    }

    @Operation(summary = "Xác minh điểm danh của thành viên")
    @PostMapping("/{eventId}/attendance/verify")
    public ResponseEntity<ApiResponse<String>> verifyAttendance(
            @PathVariable Long eventId,
            @RequestParam Long userId) {
        String msg = attendanceService.verifyAttendance(eventId, userId);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
    @Operation(summary = "Lấy thống kê tổng quan điểm danh của sự kiện")
    @GetMapping("/{eventId}/summary")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventAttendanceSummary(eventId)));
    }

    @Operation(
            summary = "Lấy danh sách sự kiện tôi đã đăng ký",
            description = """
        Dành cho **STUDENT**. 
        Trả về các sự kiện mà sinh viên đã đăng ký (kể cả đang chờ duyệt / đã duyệt / đã huỷ).
        """
    )
    @GetMapping("/my-registrations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EventRegistrationResponse>>> getMyRegisteredEvents(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<EventRegistrationResponse> events = eventService.getRegisteredEventsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }
    @Operation(
            summary = "Lấy danh sách sự kiện đã kết toán",
            description = """
            Dành cho **UNIVERSITY_STAFF**.<br>
            Liệt kê tất cả sự kiện đã hoàn tất quy trình kết toán (settlement).<br>
            Dùng cho trang thống kê hoặc theo dõi tiến trình hoàn điểm.
            """,
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách sự kiện đã kết toán thành công"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
            }
    )
    @GetMapping("/settled")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getSettledEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getSettledEvents()));
    }
    // =========================================================
// 🔹 8. EVENT FEEDBACK
// =========================================================
    @Operation(
            summary = "Sinh viên gửi feedback cho sự kiện",
            description = """
        Dành cho **STUDENT** đã tham gia sự kiện. 
        Gửi đánh giá (rating) và nội dung phản hồi sau khi sự kiện kết thúc.
        """
    )
    @PostMapping("/{eventId}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createFeedback(
            @PathVariable Long eventId,
            @RequestBody EventFeedbackRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        EventFeedbackResponse response = eventFeedbackService.createFeedback(eventId, req, userDetails);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Feedback created successfully",
                "data", response
        ));
    }



    @Operation(
            summary = "Lấy danh sách feedback của sinh viên tham gia sự kiện",
            description = """
        Dành cho **CLUB_LEADER**, **VICE_LEADER**, **UNIVERSITY_STAFF** hoặc **STUDENT**.<br>
        Trả về danh sách phản hồi của sinh viên đã tham gia sự kiện.<br>
        Dùng cho trang quản lý phản hồi của CLB hoặc thống kê đánh giá sau sự kiện.
        """
    )
    @GetMapping("/{eventId}/feedback")
    public ResponseEntity<ApiResponse<List<EventFeedbackResponse>>> getFeedbacksByEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.getFeedbacksByEvent(eventId)));
    }

    @Operation(summary = "Lấy feedback theo membership (của sinh viên)")
    @GetMapping("/memberships/{membershipId}/feedbacks")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EventFeedbackResponse>>> getFeedbacksByMember(
            @PathVariable Long membershipId) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.getFeedbacksByMembership(membershipId)));
    }
    @Operation(summary = "Cập nhật feedback sự kiện")
    @PutMapping("/feedback/{feedbackId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<EventFeedbackResponse>> updateFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody EventFeedbackRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.updateFeedback(feedbackId, req)));
    }
    @Operation(summary = "Xoá feedback sự kiện (student/admin)")
    @DeleteMapping("/feedback/{feedbackId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteFeedback(@PathVariable Long feedbackId) {
        eventFeedbackService.deleteFeedback(feedbackId);
        return ResponseEntity.ok(ApiResponse.msg("Feedback deleted successfully"));
    }
    @Operation(summary = "Tổng hợp thống kê feedback của sự kiện")
    @GetMapping("/{eventId}/feedback/summary")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedbackSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.getFeedbackSummaryByEvent(eventId)));
    }
    @Operation(
            summary = "Gia hạn hoặc điều chỉnh thời gian sự kiện (Leader/Staff)",
            description = """
        Cho phép **CLUB_LEADER** hoặc **UNIVERSITY_STAFF** thay đổi thời gian kết thúc sự kiện 
        (ví dụ: kéo dài thời gian check-in hoặc hoạt động khi sự kiện diễn ra lâu hơn dự kiến).
        """
    )
    @PutMapping("/{eventId}/extend")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<EventResponse> extendEvent(
            @PathVariable Long eventId,
            @RequestBody EventExtendRequest request) {
        return ResponseEntity.ok(eventService.extendEvent(eventId, request));
    }

    @Operation(
            summary = "Lấy tất cả feedback của các sự kiện do CLB tổ chức hoặc đồng tổ chức",
            description = """
        Dành cho **CLUB_LEADER**, **UNIVERSITY_STAFF**.<br>
        Dùng để tổng hợp phản hồi của sinh viên cho các sự kiện có sự tham gia của CLB.
        """
    )
    @GetMapping("/clubs/{clubId}/feedbacks")
    @PreAuthorize("hasAnyRole('CLUB_LEADER', 'STAFF', 'UNIVERSITY_STAFF')")
    public ResponseEntity<?> getFeedbacksByClub(@PathVariable Long clubId) {
        List<EventFeedbackResponse> res = eventFeedbackService.getFeedbacksByClub(clubId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "success",
                "data", res
        ));
    }
    @Operation(
            summary = "Duyệt ngân sách sự kiện (University Staff)",
            description = """
        Dành cho **UNIVERSITY_STAFF**.<br>
        Xác nhận và cấp ngân sách cho sự kiện, chuyển trạng thái từ **DRAFT** sang **APPROVED**.<br>
        Ghi nhận thông tin người duyệt và thời điểm phê duyệt.
        """
    )
    @PutMapping("/{eventId}/approve-budget")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<EventResponse> approveBudget(
            @PathVariable Long eventId,
            @RequestBody @Valid EventBudgetApproveRequest req,
            @AuthenticationPrincipal CustomUserDetails staff
    ) {
        return ResponseEntity.ok(eventService.approveEventBudget(eventId, req, staff));
    }
    @Operation(
            summary = "Hoàn điểm cho sinh viên khi sản phẩm sự kiện bị huỷ hoặc trả lại",
            description = """
        Dành cho **UNIVERSITY_STAFF** hoặc **ADMIN**.<br>
        Khi sinh viên đã đổi sản phẩm trong sự kiện nhưng sản phẩm bị lỗi / không sử dụng,
        hệ thống sẽ hoàn điểm tương ứng vào ví người dùng.
        """
    )
    @PutMapping("/{eventId}/refund-product/{productId}")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF') or hasRole('ADMIN')")
    public ResponseEntity<?> refundEventProduct(
            @PathVariable Long eventId,
            @PathVariable Long productId,
            @RequestParam Long userId) {
        WalletTransaction tx = eventService.refundEventProduct(eventId, userId, productId);
        return ResponseEntity.ok(tx);
    }

    @Operation(
            summary = "Xuất danh sách điểm danh (Attendance Export)",
            description = """
        Cho phép tải danh sách điểm danh của sự kiện dưới dạng CSV hoặc Excel (.xlsx).  
        - Tham số `format` có thể là `csv` hoặc `xlsx` (mặc định: csv).  
        - Kết quả bao gồm họ tên, email, trạng thái đăng ký, thời gian check-in và check-out.  
        Chức năng dành cho ban tổ chức hoặc UniStaff để thống kê và lưu trữ dữ liệu sự kiện.
        """
    )
    @GetMapping("/{eventId}/export")
    public ResponseEntity<?> exportAttendance(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        byte[] file = eventService.exportAttendanceData(eventId, format);
        String fileName = "attendance_event_" + eventId + "." + format;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(format.equals("csv")
                        ? MediaType.TEXT_PLAIN
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @Operation(
            summary = "Từ chối sự kiện (University Staff hoặc Admin)",
            description = """
        Dành cho **UNIVERSITY_STAFF** hoặc **ADMIN**.<br>
        Từ chối sự kiện không đủ điều kiện phê duyệt và ghi nhận lý do.<br>
        Nếu sự kiện có sử dụng điểm cam kết, hệ thống sẽ hoàn điểm lại cho CLB hoặc sinh viên.
        """
    )
    @PutMapping("/{eventId}/reject")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<String>> rejectEvent(
            @PathVariable Long eventId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails staff
    ) {
        String msg = eventService.rejectEvent(eventId, reason, staff);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
    @Operation(
            summary = "Lấy danh sách feedback của tôi (Student)",
            description = """
        Dành cho **STUDENT**.<br>
        Trả về toàn bộ feedback mà sinh viên đã gửi cho các sự kiện khác nhau trong hệ thống.
        Dùng để hiển thị lịch sử đánh giá của cá nhân.
        """
    )
    @GetMapping("/my-feedbacks")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EventFeedbackResponse>>> getMyFeedbacks(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<EventFeedbackResponse> feedbacks = eventFeedbackService.getFeedbacksByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(feedbacks));
    }

    @Operation(
            summary = "Đếm số lần tham gia hỗ trợ sự kiện (Staff Participation Count)",
            description = """
        API dùng để thống kê **số lần một thành viên (Membership)** tham gia hỗ trợ sự kiện (Staff).<br>
        - Hệ thống sẽ dựa vào bảng *event_staff* để đếm số lần gán staff.<br>
        - Dùng cho thống kê, xếp hạng, hoặc đánh giá mức độ đóng góp của từng thành viên.<br><br>
        
        📌 **Chỉ dành cho ADMIN / CLUB_LEADER / CLUB_MANAGER**.<br>
        Cho phép xem số lần tham gia staff của bất kỳ thành viên nào trong CLB.
        """
    )
    @GetMapping("/staff/{membershipId}/count")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER','CLUB_MANAGER')")
    public ResponseEntity<ApiResponse<Long>> countStaffParticipation(
            @PathVariable Long membershipId
    ) {
        long count = eventStaffService.countStaffParticipation(membershipId);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }


}
