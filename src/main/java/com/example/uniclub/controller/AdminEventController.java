package com.example.uniclub.controller;

import com.example.uniclub.dto.response.AdminEventResponse;
import com.example.uniclub.service.AdminEventService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    @Operation(summary = "Lấy danh sách sự kiện (phân trang + tìm kiếm)")
    @GetMapping
    public ResponseEntity<Page<AdminEventResponse>> getAllEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventId").descending());
        return ResponseEntity.ok(adminEventService.getAllEvents(keyword, pageable));
    }

    @Operation(summary = "Xem chi tiết sự kiện")
    @GetMapping("/{id}")
    public ResponseEntity<AdminEventResponse> getEventDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminEventService.getEventDetail(id));
    }

    @Operation(summary = "Duyệt sự kiện")
    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveEvent(@PathVariable Long id) {
        adminEventService.approveEvent(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Từ chối sự kiện")
    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectEvent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Vi phạm quy định tổ chức sự kiện") String reason) {
        adminEventService.rejectEvent(id, reason);
        return ResponseEntity.ok().build();
    }
}
