package com.example.uniclub.service;

import com.example.uniclub.dto.response.AdminEventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminEventService {
    Page<AdminEventResponse> getAllEvents(String keyword, Pageable pageable);
    AdminEventResponse getEventDetail(Long id);
    void approveEvent(Long id);
    void rejectEvent(Long id, String reason);
}
