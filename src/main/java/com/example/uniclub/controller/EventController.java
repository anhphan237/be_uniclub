package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.dto.response.EventWalletResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EventPointsService;
import com.example.uniclub.service.EventService;
import com.example.uniclub.service.EventStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.uniclub.service.EventWalletService;
import com.example.uniclub.service.EventSettlementService;
import com.example.uniclub.service.AttendanceService;

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
    // 🔹 1. CRUD APIs (Quản lý sự kiện)
    // =========================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.get(id)));
    }

    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(eventService.list(pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<EventResponse>> updateStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody EventStatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.updateStatus(principal, id, req.getStatus(), req.getBudgetPoints())
        ));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // =========================================================
    // 🔹 2. PARTICIPATION APIs (Đăng ký - Check-in - Hủy)
    // =========================================================

    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> register(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventRegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.register(principal, req)));
    }

    @PostMapping("/checkin")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> checkin(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventCheckinRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.checkin(principal, req)));
    }

    @PutMapping("/{eventId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> cancelRegistration(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.cancelRegistration(principal, eventId)));
    }

    @PutMapping("/end")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> endEvent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody EventEndRequest req) {
        return ResponseEntity.ok(ApiResponse.msg(eventPointsService.endEvent(principal, req)));
    }

    // =========================================================
    // 🔹 3. LOOKUP APIs (Tra cứu)
    // =========================================================

    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<EventResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getByClubId(clubId));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<ApiResponse<EventResponse>> getByCheckInCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCheckInCode(code)));
    }

    @GetMapping("/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventRegistrations(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventRegistrations(eventId)));
    }

    @GetMapping("/{eventId}/summary")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventSummary(eventId)));
    }

    @GetMapping("/{eventId}/wallet")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventWallet(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventWallet(eventId)));
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getUpcomingEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getUpcomingEvents()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getMyEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getMyEvents(principal)));
    }

    @PostMapping("/{eventId}/clone")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> cloneEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.cloneEvent(eventId)));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> filter(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) EventStatusEnum status,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.filter(name, date, status, pageable));
    }

    // =========================================================
    // 🔹 5. STAFF MANAGEMENT (Gộp từ EventStaffController)
    // =========================================================

    /**
     * [18] Gán Staff cho Sự kiện
     * Roles: CLUB_LEADER, CLUB_VICE_LEADER
     * Method: POST /api/events/{id}/staffs
     */
    @PostMapping("/{id}/staffs")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<EventStaffResponse>> assignStaff(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @RequestParam Long membershipId,
            @RequestParam(required = false) String duty) {

        return ResponseEntity.ok(ApiResponse.ok(
                eventStaffService.assignStaff(id, membershipId, duty)
        ));
    }

    /**
     * [19] Gỡ Staff khỏi Sự kiện
     * Roles: CLUB_LEADER, CLUB_VICE_LEADER
     * Method: DELETE /api/events/{id}/staffs/{staffId}
     */
    @DeleteMapping("/{id}/staffs/{staffId}")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> unassignStaff(
            @PathVariable Long id,
            @PathVariable Long staffId) {
        eventStaffService.unassignStaff(staffId);
        return ResponseEntity.ok(ApiResponse.msg("Staff unassigned successfully"));
    }

    /**
     * [20] Xem danh sách Staff của Sự kiện
     * Roles: ADMIN, UNIVERSITY_STAFF, CLUB_LEADER, CLUB_VICE_LEADER
     * Method: GET /api/events/{id}/staffs
     */
    @GetMapping("/{id}/staffs")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<EventStaffResponse>>> getEventStaffs(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventStaffList(id)));
    }
//    21 lấy co club

    @GetMapping("/club/{clubId}/cohost")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<EventResponse>> getCoHostedEvents(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getCoHostedEvents(clubId));
    }
// =========================================================
// 🔹 6. EVENT APPROVE + SETTLE
// =========================================================

    @PostMapping("/{eventId}/approve")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> approveEvent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId,
            @RequestParam(required = false) Integer budgetPoints) {
        String msg = eventService.reviewByUniStaff(eventId, true, principal, budgetPoints);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }


    @PostMapping("/{eventId}/settle")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> settleEvent(@PathVariable Long eventId) {
        Event event = eventService.getEntity(eventId);
        eventSettlementService.settleEvent(event);
        eventWalletService.returnSurplusToClubs(event);
        return ResponseEntity.ok(ApiResponse.msg("Event settled successfully"));
    }
    @PostMapping("/{eventId}/attendance/verify")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> verifyAttendance(
            @PathVariable Long eventId,
            @RequestParam Long userId) {
        String msg = attendanceService.verifyAttendance(eventId, userId);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
// =========================================================
// 🔹 7. CO-HOST CONFIRMATION & SUBMIT TO UNISTAFF
// =========================================================

    @PostMapping("/{eventId}/cohost/accept")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> acceptCohost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        String msg = eventService.acceptCohost(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    @PostMapping("/{eventId}/cohost/reject")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> rejectCohost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        String msg = eventService.rejectCohost(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    @PutMapping("/{eventId}/submit")
    @PreAuthorize("hasAnyRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> submitEventToUniStaff(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        String msg = eventService.submitEventToUniStaff(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
    @GetMapping("/{eventId}/wallet/detail")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER','ADMIN')")
    public ResponseEntity<ApiResponse<EventWalletResponse>> getEventWalletDetail(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventWalletService.getEventWalletDetail(eventId)));
    }
    @GetMapping("/{eventId}/attendance/qr")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAttendanceQr(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getQrTokenForEvent(eventId)));
    }
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-registrations")
    public ApiResponse<?> getMyRegisteredEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return new ApiResponse<>(true, "success", eventPointsService.getMyRegisteredEvents(principal));
    }
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER','UNIVERSITY_STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<?>> getActiveEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getActiveEvents()));
    }
    @PostMapping("/{eventId}/complete")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> completeEvent(@PathVariable Long eventId) {
        String msg = eventService.markEventCompleted(eventId);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }


}
