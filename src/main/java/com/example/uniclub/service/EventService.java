package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.uniclub.entity.Event;
import java.time.LocalDate;
import java.util.List;

public interface EventService {

    EventResponse create(EventCreateRequest req);

    EventResponse get(Long id);

    Page<EventResponse> list(Pageable pageable);

    EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status, Integer budgetPoints);

    void delete(Long id);

    EventResponse findByCheckInCode(String code);

    List<EventResponse> getByClubId(Long clubId);

    // New extended APIs
    List<EventResponse> getUpcomingEvents();

    List<EventResponse> getMyEvents(CustomUserDetails principal);

    EventResponse cloneEvent(Long eventId);

    Page<EventResponse> filter(String name, LocalDate date, EventStatusEnum status, Pageable pageable);

    EventResponse assignStaff(CustomUserDetails principal, Long eventId, Long membershipId, String duty);

    List<Membership> getEventStaffs(CustomUserDetails principal, Long eventId);

    List<EventStaffResponse> getEventStaffList(Long eventId);

    List<EventResponse> getCoHostedEvents(Long clubId);
    Event getEntity(Long id);
    String acceptCohost(Long eventId, CustomUserDetails principal);
    String rejectCohost(Long eventId, CustomUserDetails principal);
    String submitEventToUniStaff(Long eventId, CustomUserDetails principal);


}
