package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventService {

    EventResponse create(EventCreateRequest req);

    EventResponse get(Long id);

    Page<EventResponse> list(Pageable pageable);

    EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status);

    void delete(Long id);

    EventResponse findByCheckInCode(String code);

    List<EventResponse> getByClubId(Long clubId);

    // New extended APIs
    List<EventResponse> getUpcomingEvents();

    List<EventResponse> getMyEvents(CustomUserDetails principal);

    EventResponse cloneEvent(Long eventId);
}
