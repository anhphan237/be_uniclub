package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
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

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventPointsService eventPointsService;
    private final EventStaffService eventStaffService;

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
                eventService.updateStatus(principal, id, req.getStatus())
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
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
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
    @PreAuthorize("hasAnyRole('CLUB_LEADER','CLUB_VICE_LEADER')")
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
    @PreAuthorize("hasAnyRole('CLUB_LEADER','CLUB_VICE_LEADER')")
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
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','CLUB_VICE_LEADER')")
    public ResponseEntity<ApiResponse<List<EventStaffResponse>>> getEventStaffs(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventStaffList(id)));
    }
}
