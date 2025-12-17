package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventBudgetApproveRequest;
import com.example.uniclub.dto.request.EventCancelRequest;
import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.request.EventExtendRequest;
import com.example.uniclub.dto.response.EventRegistrationResponse;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface EventService {

    // =========================================================
    // 🔹 CRUD
    // =========================================================
    EventResponse create(EventCreateRequest req);

    EventResponse get(Long id);

    Page<EventResponse> list(Pageable pageable);

    Page<EventResponse> filter(String name, LocalDate date, EventStatusEnum status, Pageable pageable);

    void delete(Long id);

    // =========================================================
    // 🔹 TRA CỨU
    // =========================================================
    EventResponse findByCheckInCode(String code);

    List<EventResponse> getByClubId(Long clubId);

    List<EventResponse> getUpcomingEvents();

    List<EventResponse> getMyEvents(CustomUserDetails principal);

    List<EventResponse> getActiveEvents();

    List<EventResponse> getCoHostedEvents(Long clubId);

    List<EventResponse> getAllEvents();

    Event getEntity(Long id);

    // =========================================================
    // 🔹 CO-HOST HANDLING
    // =========================================================
    String respondCoHost(Long eventId, CustomUserDetails principal, boolean accepted);
    List<EventResponse> getEventsByLocation(Long locationId);

    // =========================================================
    // 🔹 EVENT LIFECYCLE
    // =========================================================
    String finishEvent(Long eventId, CustomUserDetails principal);
    String rejectEvent(Long eventId, String reason, CustomUserDetails staff);
    // =========================================================
    // 🔹 STAFF MANAGEMENT
    // =========================================================
    EventResponse assignStaff(CustomUserDetails principal, Long eventId, Long membershipId, String duty);

    List<EventStaffResponse> getEventStaffList(Long eventId);

    List<EventRegistrationResponse> getRegisteredEventsByUser(Long userId);
    List<EventResponse> getSettledEvents();

    EventResponse extendEvent(Long eventId, EventExtendRequest request);

    EventResponse approveEventBudget(Long eventId, EventBudgetApproveRequest req, CustomUserDetails staff);

    WalletTransaction refundEventProduct(Long eventId, Long userId, Long productId);

    byte[] exportAttendanceData(Long eventId, String format);
    Map<String, Object> getEventAttendanceSummary(Long eventId);
    public String cancelEvent(Long eventId, EventCancelRequest req, CustomUserDetails principal);
    List<EventResponse> getEventsByDate(LocalDate date);

}
