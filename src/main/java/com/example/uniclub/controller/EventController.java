package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
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
    private final EventFeedbackService eventFeedbackService;

    // =========================================================
    // 🔹 1. CRUD
    // =========================================================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.get(id)));
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> list(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.list(pageable));
    }
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(
                eventService.getAllEvents()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }

    // =========================================================
    // 🔹 2. PARTICIPATION
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

    @PostMapping("/{eventId}/complete")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> completeEvent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        String msg = eventService.finishEvent(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }


    // =========================================================
    // 🔹 3. LOOKUP
    // =========================================================
    @GetMapping("/club/{clubId}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF','CLUB_LEADER','STUDENT')")
    public ResponseEntity<List<EventResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getByClubId(clubId));
    }

    @GetMapping("/club/{clubId}/cohost")
    public ResponseEntity<List<EventResponse>> getCoHostedEvents(@PathVariable Long clubId) {
        return ResponseEntity.ok(eventService.getCoHostedEvents(clubId));
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<EventResponse>> getByCheckInCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCheckInCode(code)));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<EventResponse>> filter(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) EventStatusEnum status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(eventService.filter(name, date, status, pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<?>> getUpcomingEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getUpcomingEvents()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getActiveEvents()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<?>> getMyEvents(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getMyEvents(principal)));
    }

    // =========================================================
    // 🔹 4. CO-HOST CONFIRMATION
    // =========================================================
    @PostMapping("/{eventId}/cohost/respond")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<String>> respondCohost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId,
            @RequestParam boolean accept) {
        String msg = eventService.respondCoHost(eventId, principal, accept);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    @PutMapping("/{eventId}/submit")
    @PreAuthorize("hasRole('CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> submitEventToUniStaff(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long eventId) {
        String msg = eventService.submitEventToUniStaff(eventId, principal);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }

    // =========================================================
    // 🔹 5. STAFF MANAGEMENT
    // =========================================================
    @PostMapping("/{id}/staffs")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER')")
    public ResponseEntity<ApiResponse<EventStaffResponse>> assignStaff(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @RequestParam Long membershipId,
            @RequestParam(required = false) String duty) {
        return ResponseEntity.ok(ApiResponse.ok(eventStaffService.assignStaff(id, membershipId, duty)));
    }

    @DeleteMapping("/{id}/staffs/{staffId}")
    public ResponseEntity<ApiResponse<String>> unassignStaff(
            @PathVariable Long id, @PathVariable Long staffId) {
        eventStaffService.unassignStaff(staffId);
        return ResponseEntity.ok(ApiResponse.msg("Staff unassigned successfully"));
    }

    @GetMapping("/{id}/staffs")
    public ResponseEntity<ApiResponse<List<EventStaffResponse>>> getEventStaffs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventStaffList(id)));
    }

    // =========================================================
    // 🔹 6. EVENT APPROVAL & SETTLEMENT
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
    @GetMapping("/{eventId}/wallet/detail")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER','ADMIN')")
    public ResponseEntity<ApiResponse<EventWalletResponse>> getEventWalletDetail(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventWalletService.getEventWalletDetail(eventId)));
    }

    // =========================================================
    // 🔹 7. ATTENDANCE
    // =========================================================
    @GetMapping("/{eventId}/attendance/qr")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendanceQr(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "START") String phase) {

        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getQrTokenForEvent(eventId, phase)));
    }


    @PostMapping("/{eventId}/attendance/verify")
    public ResponseEntity<ApiResponse<String>> verifyAttendance(
            @PathVariable Long eventId,
            @RequestParam Long userId) {
        String msg = attendanceService.verifyAttendance(eventId, userId);
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<String>> updateEventStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody EventStatusUpdateRequest req) {

        boolean approve = req.getStatus() == EventStatusEnum.APPROVED;
        String msg = eventService.reviewByUniStaff(id, approve, principal, req.getBudgetPoints());
        return ResponseEntity.ok(ApiResponse.msg(msg));
    }
    @GetMapping("/{eventId}/summary")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<?>> getEventSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventPointsService.getEventSummary(eventId)));
    }
    @GetMapping("/my-registrations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EventRegistrationResponse>>> getMyRegisteredEvents(
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<EventRegistrationResponse> events = eventService.getRegisteredEventsByUser(userId);
        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    @GetMapping("/settled")
    @PreAuthorize("hasRole('UNIVERSITY_STAFF')")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getSettledEvents() {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getSettledEvents()));
    }
    // =========================================================
// 🔹 8. EVENT FEEDBACK
// =========================================================
    @PostMapping("/{eventId}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<EventFeedbackResponse>> createFeedback(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody EventFeedbackRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.createFeedback(eventId, req, user)));
    }


    @GetMapping("/{eventId}/feedback")
    public ResponseEntity<ApiResponse<List<EventFeedbackResponse>>> getFeedbacksByEvent(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.getFeedbacksByEvent(eventId)));
    }

    // ✅ NEW
    @GetMapping("/memberships/{membershipId}/feedbacks")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<EventFeedbackResponse>>> getFeedbacksByMember(
            @PathVariable Long membershipId) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.getFeedbacksByMembership(membershipId)));
    }

    @PutMapping("/feedback/{feedbackId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<EventFeedbackResponse>> updateFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody EventFeedbackRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.updateFeedback(feedbackId, req)));
    }

    @DeleteMapping("/feedback/{feedbackId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteFeedback(@PathVariable Long feedbackId) {
        eventFeedbackService.deleteFeedback(feedbackId);
        return ResponseEntity.ok(ApiResponse.msg("Feedback deleted successfully"));
    }

    @GetMapping("/{eventId}/feedback/summary")
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeedbackSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.ok(eventFeedbackService.getFeedbackSummaryByEvent(eventId)));
    }

    @PutMapping("/{eventId}/extend")
    @PreAuthorize("hasAnyRole('CLUB_LEADER','UNIVERSITY_STAFF')")
    public ResponseEntity<EventResponse> extendEvent(
            @PathVariable Long eventId,
            @RequestBody EventExtendRequest request) {
        return ResponseEntity.ok(eventService.extendEvent(eventId, request));
    }


}
