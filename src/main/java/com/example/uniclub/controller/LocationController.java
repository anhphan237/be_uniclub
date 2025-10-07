package com.example.uniclub.controller;

import com.example.uniclub.dto.request.LocationCreateRequest;
import com.example.uniclub.dto.response.LocationResponse;
import com.example.uniclub.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationResponse> create(@RequestBody @Valid LocationCreateRequest req) {
        return ResponseEntity.ok(locationService.create(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.get(id));
    }

    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(locationService.list(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
