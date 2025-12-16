package com.example.uniclub.controller;

import com.example.uniclub.dto.response.LocationLogResponse;
import com.example.uniclub.service.LocationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Tag(name = "Location Logs", description = "Endpoints to query location logs by event and time")
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationLogController {

    private final LocationLogService locationLogService;

    @GetMapping("/{id}/logs")
    @Operation(
            summary = "Get location logs filtered by event and time",
            description = "Returns logs for a location. Filters: eventId (optional), startDate/endDate (dd-MM-yyyy), startTime/endTime (HH:mm)."
    )
    public ResponseEntity<?> getLocationLogs(
            @PathVariable Long id,

            @RequestParam(required = false)
            @Parameter(description = "Filter by event id")
            Long eventId,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            @Parameter(description = "Start date (dd-MM-yyyy)", example = "20-12-2025")
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            @Parameter(description = "End date (dd-MM-yyyy)", example = "22-12-2025")
            LocalDate endDate,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm")
            @Parameter(description = "Start time (HH:mm)", example = "09:00", schema = @Schema(type = "string", format = "time"))
            LocalTime startTime,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm")
            @Parameter(description = "End time (HH:mm)", example = "11:00", schema = @Schema(type = "string", format = "time"))
            LocalTime endTime,

            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
                locationLogService.getLogs(id, eventId, startDate, endDate, startTime, endTime, pageable)
        );
    }
}
