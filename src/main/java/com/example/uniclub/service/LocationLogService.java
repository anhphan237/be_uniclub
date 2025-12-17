package com.example.uniclub.service;

import com.example.uniclub.dto.response.LocationDailyLogResponse;
import com.example.uniclub.dto.response.LocationEventTimeResponse;
import com.example.uniclub.dto.response.LocationLogResponse;
import com.example.uniclub.repository.LocationEventHistoryRepository;
import com.example.uniclub.repository.LocationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    private final LocationEventHistoryRepository historyRepo;

    public List<LocationDailyLogResponse> getLocationDailyHistory(
            Long locationId,
            Long eventId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        // default để tránh null
        LocalDate startD = (startDate != null) ? startDate : LocalDate.of(1900, 1, 1);
        LocalDate endD   = (endDate != null)   ? endDate   : LocalDate.of(2999, 12, 31);
        LocalTime startT = (startTime != null) ? startTime : LocalTime.MIN;
        LocalTime endT   = (endTime != null)   ? endTime   : LocalTime.MAX;

        List<LocationLogResponse> flatLogs =
                historyRepo.findFlatLogs(locationId, eventId, startD, endD, startT, endT);

        // group theo ngày
        return flatLogs.stream()
                .collect(Collectors.groupingBy(
                        LocationLogResponse::getDate,
                        HashMap::new,
                        Collectors.mapping(
                                l -> new LocationEventTimeResponse(
                                        l.getEventId(),
                                        l.getEventName(),
                                        l.getStartTime(),
                                        l.getEndTime()
                                ),
                                Collectors.toList()
                        )
                ))
                .entrySet()
                .stream()
                .map(e -> new LocationDailyLogResponse(e.getKey(), e.getValue()))
                .toList();
    }
}
