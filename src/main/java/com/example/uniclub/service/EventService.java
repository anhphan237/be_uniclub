package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventRegistrationResponse;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

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

    List<EventResponse> getAllEvents(); // ✅ Lấy toàn bộ event (không phân trang)

    Event getEntity(Long id);

    // =========================================================
    // 🔹 CO-HOST HANDLING
    // =========================================================
    String respondCoHost(Long eventId, CustomUserDetails principal, boolean accepted);

    String submitEventToUniStaff(Long eventId, CustomUserDetails principal);

    // =========================================================
    // 🔹 UNISTAFF APPROVAL
    // =========================================================
    String reviewByUniStaff(Long eventId, boolean approve, CustomUserDetails principal, Integer budgetPoints);

    // =========================================================
    // 🔹 EVENT LIFECYCLE
    // =========================================================
    String finishEvent(Long eventId, CustomUserDetails principal);

    // =========================================================
    // 🔹 STAFF MANAGEMENT
    // =========================================================
    EventResponse assignStaff(CustomUserDetails principal, Long eventId, Long membershipId, String duty);

    List<EventStaffResponse> getEventStaffList(Long eventId);

    List<EventRegistrationResponse> getRegisteredEventsByUser(Long userId);
    List<EventResponse> getSettledEvents();


}
