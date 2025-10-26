package com.example.uniclub.controller;

import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.service.EventCoClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event-cohosts")
@RequiredArgsConstructor
public class EventCoClubController {

    private final EventCoClubService eventCoClubService;

    @PatchMapping("/{eventId}/{clubId}/status")
    public ResponseEntity<String> updateCoHostStatus(
            @PathVariable Long eventId,
            @PathVariable Long clubId,
            @RequestParam EventCoHostStatusEnum status) {

        eventCoClubService.updateStatus(eventId, clubId, status);
        return ResponseEntity.ok("Cập nhật trạng thái thành công: " + status);
    }
}
