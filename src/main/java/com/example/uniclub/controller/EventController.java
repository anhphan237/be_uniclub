package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.dto.response.EventWalletResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventPointsService eventPointsService;
    private final EventStaffService eventStaffService;
    private final EventWalletService eventWalletService;
    private final EventSettlementService eventSettlementService;
    private final AttendanceService attendanceService;

    // =========================================================
    // 🔹 1. CRUD APIs
    // =========================================================

    /** 🟢 Tạo mới sự kiện (Admin / Leader) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.create(req)));
    }

    /** 🟢 Lấy chi tiết 1 sự kiện */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.get(id)));
    }

    /** 🟢 Lấy danh sách tất cả sự kiện (phân trang) */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.list(pageable));
    }

    /** 🟢 UniStaff duyệt / từ chối sự kiện */
    @PutMapping("/events/{id}/status")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long id,
            @RequestBody EventStatusUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {

        String msg;
        if (req.getStatus() == EventStatusEnum.APPROVED) {
            msg = eventService.reviewByUniStaff(id, true, principal, req.getBudgetPoints());
        } else if (req.getStatus() == EventStatusEnum.REJECTED) {
            msg = eventService.reviewByUniStaff(id, false, principal, null);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ UniStaff được set APPROVED/REJECTED.");
        }
        return ResponseEntity.ok(new ApiResponse<>(true, msg, null));
    }


    /** 🟢 Xóa sự kiện (Admin / Leader) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // =========================================================
    // 🔹 2. PARTICIPATION
    // =========================================================

    /** 🟢 Sinh viên đăng ký tham gia sự kiện */
    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> register(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody EventRegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.register(principal, req)));
    }

    /** 🟢 Sinh viên check-in tham gia sự kiện */
    @PostMapping("/checkin")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> checkin(@AuthenticationPrincipal CustomUserDetails principal,
                                                       @Valid @RequestBody EventCheckinRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.checkin(principal, req)));
    }

    /** 🟢 Sinh viên hủy đăng ký sự kiện */
    @PutMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> cancelRegistration(@AuthenticationPrincipal CustomUserDetails principal,
                                                                  @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.cancelRegistration(principal, eventId)));
    }

    /** 🟢 Kết thúc sự kiện (Leader / UniStaff) */
    @PutMapping("/end")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> endEvent(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @Valid @RequestBody EventEndRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.endEvent(principal, req)));
    }

    // =========================================================
    // 🔹 3. LOOKUP
    // =========================================================

    /** 🟢 Lấy danh sách sự kiện của 1 CLB */
    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<EventResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getByClubId(clubId));
    }

    /** 🟢 Lấy sự kiện bằng mã check-in */
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<EventResponse>> getByCheckInCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCheckInCode(code)));
    }

    /** 🟢 Lấy danh sách đăng ký của 1 sự kiện */
    @GetMapping("/{eventId}/registrations")
    public ResponseEntity<ApiResponse<?>> getEventRegistrations(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventRegistrations(eventId)));
    }

    /** 🟢 Tóm tắt sự kiện (thống kê tham gia, điểm, ví...) */
    @GetMapping("/{eventId}/summary")
    public ResponseEntity<ApiResponse<?>> getEventSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventSummary(eventId)));
    }

    /** 🟢 Lấy chi tiết ví của sự kiện */
    @GetMapping("/{eventId}/wallet/detail")
    public ResponseEntity<ApiResponse<EventWalletResponse>> getEventWalletDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventWalletService.getEventWalletDetail(eventId)));
    }

    /** 🟢 Lấy danh sách sự kiện sắp diễn ra (Student) */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<?>> getUpcomingEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getUpcomingEvents()));
    }

    /** 🟢 Lấy danh sách sự kiện đang hoạt động */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getActiveEvents()));
    }

    /** 🟢 Lấy danh sách sự kiện của bản thân (Student) */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getMyEvents(principal)));
    }

    /** 🟢 Lấy danh sách sự kiện đã đăng ký (Student) */
    @GetMapping("/my-registrations")
    public ApiResponse<?> getMyRegisteredEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return new ApiResponse<>(true, "success", eventPointsService.getMyRegisteredEvents(principal));
    }

    /** 🟢 Lọc sự kiện theo tên / ngày / trạng thái */
    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> filter(@RequestParam(required = false) String name,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                      @RequestParam(required = false) EventStatusEnum status,
                                                      @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.filter(name, date, status, pageable));
    }

    /** 🟢 Lấy danh sách sự kiện đồng tổ chức của 1 CLB */
    @GetMapping("/club/{clubId}/cohost")
    public ResponseEntity<List<EventResponse>> getCoHostedEvents(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getCoHostedEvents(clubId));
    }

    // =========================================================
    // 🔹 4. SETTLEMENT & COMPLETE
    // =========================================================

    /** 🟢 UniStaff thực hiện settle (chốt điểm + hoàn ví) */
    @PostMapping("/{eventId}/settle")
    public ResponseEntity<ApiResponse<String>> settleEvent(@PathVariable Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventSettlementService.settleEvent(event);
        eventWalletService.returnSurplusToClubs(event);
        return ResponseEntity.ok(ApiResponse.msg("Event settled successfully"));
    }

    /** 🟢 Đánh dấu hoàn tất sự kiện (Leader / UniStaff) */
    @PostMapping("/{eventId}/complete")
    public ResponseEntity<ApiResponse<String>> completeEvent(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @PathVariable Long eventId) {
        String msg = eventService.finishEvent(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    // =========================================================
    // 🔹 5. CO-HOST
    // =========================================================

    /** 🟢 CLB đồng tổ chức phản hồi lời mời (đồng ý / từ chối) */
    @PostMapping("/{eventId}/cohost/respond")
    public ResponseEntity<ApiResponse<String>> respondCohost(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @PathVariable Long eventId,
                                                             @RequestParam boolean accept) {
        String msg = eventService.respondCoHost(eventId, principal, accept);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    /** 🟢 Leader gửi sự kiện đã duyệt co-host lên UniStaff */
    @PutMapping("/{eventId}/submit")
    public ResponseEntity<ApiResponse<String>> submitEventToUniStaff(@AuthenticationPrincipal CustomUserDetails principal,
                                                                     @PathVariable Long eventId) {
        String msg = eventService.submitEventToUniStaff(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    // =========================================================
    // 🔹 6. STAFF
    // =========================================================

    /** 🟢 Phân công thành viên làm staff sự kiện */
    @PostMapping("/{id}/staffs")
    public ResponseEntity<ApiResponse<EventStaffResponse>> assignStaff(@AuthenticationPrincipal CustomUserDetails principal,
                                                                       @PathVariable Long id,
                                                                       @RequestParam Long membershipId,
                                                                       @RequestParam(required = false) String duty) {
        return ResponseEntity.ok(ApiResponse.ok(eventStaffService.assignStaff(id, membershipId, duty)));
    }

    /** 🟢 Gỡ staff khỏi sự kiện */
    @DeleteMapping("/{id}/staffs/{staffId}")
    public ResponseEntity<ApiResponse<String>> unassignStaff(@PathVariable Long id, @PathVariable Long staffId) {
        eventStaffService.unassignStaff(staffId);
        return ResponseEntity.ok(ApiResponse.msg("Staff unassigned successfully"));
    }

    /** 🟢 Lấy danh sách staff của sự kiện */
    @GetMapping("/{id}/staffs")
    public ResponseEntity<ApiResponse<List<EventStaffResponse>>> getEventStaffs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventStaffList(id)));
    }

    // =========================================================
    // 🔹 7. ATTENDANCE
    // =========================================================

    /** 🟢 Lấy QR điểm danh (leader / staff) */
    @GetMapping("/{eventId}/attendance/qr")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceQr(@PathVariable Long eventId,
                                                                            @RequestParam(defaultValue = "START") String phase) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getQrTokenForEvent(eventId, phase)));
    }

    /** 🟢 Xác minh điểm danh bằng userId */
    @PostMapping("/{eventId}/attendance/verify")
    public ResponseEntity<ApiResponse<String>> verifyAttendance(@PathVariable Long eventId, @RequestParam Long userId) {
        String msg = attendanceService.verifyAttendance(eventId, userId);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
}
