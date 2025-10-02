package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.request.ClubCreateRequest;
import com.example.uniclub.dto.response.ClubResponse;
import com.example.uniclub.service.ClubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {
    private final ClubService clubService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClubResponse>> create(@Valid @RequestBody ClubCreateRequest req){
        return ResponseEntity.ok(ApiResponse.ok(clubService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClubResponse>> get(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.ok(clubService.get(id)));
    }

    @GetMapping
    public ResponseEntity<?> list(Pageable pageable){
        return ResponseEntity.ok(clubService.list(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id){
        clubService.delete(id);
        return ResponseEntity.ok(ApiResponse.msg("Deleted"));
    }
}
