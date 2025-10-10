package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.enums.EventStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {
    EventResponse create(EventCreateRequest req);
    EventResponse get(Long id);
    Page<EventResponse> list(Pageable pageable);
    EventResponse updateStatus(Long id, EventStatusEnum status);
    EventResponse findByCheckInCode(String code); // ✅ Tìm theo mã check-in
    void delete(Long id);
}
