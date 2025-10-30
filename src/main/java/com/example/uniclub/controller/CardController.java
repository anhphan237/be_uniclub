package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.CardRequest;
import com.example.uniclub.dto.response.CardResponse;
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

    // 🟢 Tạo hoặc cập nhật Card cho Club
    @PreAuthorize("hasAnyRole('CLUB_LEADER','VICE_LEADER','ADMIN','UNIVERSITY_STAFF')")
    @PostMapping("/{clubId}")
    public ResponseEntity<ApiResponse<CardResponse>> saveOrUpdate(
            @PathVariable Long clubId,
            @RequestBody CardRequest req) {

        return ResponseEntity.ok(cardService.saveOrUpdate(clubId, req));
    }

    // 🔵 Lấy Card theo ClubId (cho Leader, Member, Student, Staff, Admin)
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF','ADMIN')")
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ApiResponse<CardResponse>> getByClubId(@PathVariable Long clubId) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getByClubId(clubId)));
    }

    // 🔴 Xóa Card
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY_STAFF')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(cardService.delete(id));
    }
    // GET theo cardId (cho mọi role đọc)
    @PreAuthorize("hasAnyRole('STUDENT','CLUB_LEADER','VICE_LEADER','UNIVERSITY_STAFF','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getById(id)));
    }

    // GET all (chỉ ADMIN & STAFF)
    @PreAuthorize("hasAnyRole('UNIVERSITY_STAFF','ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getAll()));
    }

}
