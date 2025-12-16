package com.example.uniclub.service;

import com.example.uniclub.dto.response.LocationLogResponse;
import com.example.uniclub.repository.LocationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class LocationLogService {

    private final LocationLogRepository locationLogRepository;

    public Page<LocationLogResponse> getLogs(
            Long locationId,
            Long eventId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            Pageable pageable
    ) {
        return locationLogRepository.findLogs(locationId, eventId, startDate, endDate, startTime, endTime, pageable);
    }
}
