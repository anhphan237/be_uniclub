package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.request.EventStatusUpdateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody EventCreateRequest req) {
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
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<EventResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventStatusUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.updateStatus(id, req.getStatus())
        ));
    }

    // ✅ Check-in theo mã code
    @GetMapping("/checkin/{code}")
    public ResponseEntity<ApiResponse<EventResponse>> checkInByCode(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.findByCheckInCode(code)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLUB_LEADER')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }
}
