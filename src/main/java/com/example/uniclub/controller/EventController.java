package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EventPointsService;
import com.example.uniclub.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventPointsService eventPointsService;

    // =========================================================
    // 🔹 1. CRUD APIs (Quản lý sự kiện)
    // =========================================================

    /**
     * [1] Tạo mới sự kiện
     * Roles: ADMIN, CLUB_LEADER
     * Method: POST /api/events
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.create(req)));
    }


    /**
     * [2] Xem chi tiết một sự kiện
     * Roles: Public (ai cũng xem được)
     * Method: GET /api/events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.get(id)));
    }

    /**
     * [3] Danh sách tất cả sự kiện (có phân trang)
     * Roles: Public
     * Method: GET /api/events
     */
    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(eventService.list(pageable));
    }

    /**
     * [4] Cập nhật trạng thái sự kiện (duyệt / từ chối)
     * Roles: UNIVERSITY_STAFF
     * Method: PUT /api/events/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<EventResponse>> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody EventStatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.updateStatus(principal, id, req.getStatus())
        ));
    }

    /**
     * [5] Xóa sự kiện
     * Roles: ADMIN, CLUB_LEADER
     * Method: DELETE /api/events/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // =========================================================
    // 🔹 2. PARTICIPATION APIs (Đăng ký - Check-in - Hủy)
    // =========================================================

    /**
     * [6] Đăng ký tham gia sự kiện
     * Roles: STUDENT
     * Method: POST /api/events/register
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> register(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventRegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.register(principal, req)));
    }

    /**
     * [7] Check-in tham gia sự kiện (quét mã QR)
     * Roles: STUDENT
     * Method: POST /api/events/checkin
     */
    @PostMapping("/checkin")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> checkin(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventCheckinRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.checkin(principal, req)));
    }

    /**
     * [8] Hủy đăng ký sự kiện
     * Roles: STUDENT
     * Method: PUT /api/events/{eventId}/cancel
     */
    @PutMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> cancelRegistration(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.cancelRegistration(principal, eventId)));
    }

    /**
     * [9] Kết thúc sự kiện - tính điểm thưởng, hoàn điểm
     * Roles: CLUB_LEADER, UNIVERSITY_STAFF
     * Method: PUT /api/events/end
     */
    @PutMapping("/end")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> endEvent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventEndRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.endEvent(principal, req)));
    }

    // =========================================================
    // 🔹 3. LOOKUP APIs (Truy vấn, tra cứu)
    // =========================================================

    /**
     * [10] Lấy danh sách sự kiện của một CLB cụ thể
     * Roles: ADMIN, UNIVERSITY_STAFF, CLUB_LEADER, STUDENT
     * Method: GET /api/events/club/{clubId}
     */
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<EventResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getByClubId(clubId));
    }

    /**
     * [11] Tìm sự kiện qua mã check-in (QR code)
     * Roles: ADMIN, UNIVERSITY_STAFF, CLUB_LEADER, STUDENT
     * Method: GET /api/events/code/{code}
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<EventResponse>> getByCheckInCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCheckInCode(code)));
    }

    // =========================================================
    // 🔹 4. EXTENSIONS (Báo cáo, thống kê, nhân bản)
    // =========================================================

    /**
     * [12] Xem danh sách người đăng ký của sự kiện
     * Roles: CLUB_LEADER, UNIVERSITY_STAFF
     * Method: GET /api/events/{eventId}/registrations
     */
    @GetMapping("/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventRegistrations(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventRegistrations(eventId)));
    }

    /**
     * [13] Xem thống kê sự kiện (tổng người tham gia, điểm đã chi, v.v.)
     * Roles: CLUB_LEADER, UNIVERSITY_STAFF
     * Method: GET /api/events/{eventId}/summary
     */
    @GetMapping("/{eventId}/summary")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventSummary(eventId)));
    }

    /**
     * [14] Xem ví điểm của sự kiện (chỉ staff được xem)
     * Roles: UNIVERSITY_STAFF
     * Method: GET /api/events/{eventId}/wallet
     */
    @GetMapping("/{eventId}/wallet")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventWallet(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventWallet(eventId)));
    }

    /**
     * [15] Lấy danh sách sự kiện sắp tới (Upcoming)
     * Roles: STUDENT
     * Method: GET /api/events/upcoming
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getUpcomingEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getUpcomingEvents()));
    }

    /**
     * [16] Lấy danh sách sự kiện cá nhân của người dùng
     * Roles: STUDENT
     * Method: GET /api/events/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getMyEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getMyEvents(principal)));
    }

    /**
     * [17] Nhân bản sự kiện sang học kỳ kế tiếp
     * Roles: CLUB_LEADER
     * Method: POST /api/events/{eventId}/clone
     */
    @PostMapping("/{eventId}/clone")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> cloneEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.cloneEvent(eventId)));
    }
}
