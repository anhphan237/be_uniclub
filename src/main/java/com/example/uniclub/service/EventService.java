package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {
    EventResponse create(EventCreateRequest req);
    EventResponse get(Long id);
    Page<EventResponse> list(Pageable pageable);
    void delete(Long id);
}
