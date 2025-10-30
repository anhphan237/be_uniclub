package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.entity.Card;
import com.example.uniclub.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // 🟢 Tạo hoặc cập nhật card cho club
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','ADMIN','UNIVERSITY_STAFF')")
    @PostMapping("/{clubId}")
    public ResponseEntity<ApiResponse<Card>> saveOrUpdate(
            @PathVariable Long clubId,
            @RequestBody Card req) {
        return ResponseEntity.ok(cardService.saveOrUpdate(clubId, req));
    }

    // 🔵 Lấy card theo club
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ApiResponse<List<Card>>> getByClub(@PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getByClub(clubId)));
    }

    // 🟣 Lấy card theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Card>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getById(id)));
    }

    // 🔴 Xóa card
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.delete(id));
    }
}
