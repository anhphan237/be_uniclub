package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.LocationCreateRequest;
import com.example.uniclub.dto.request.LocationUpdateRequest;
import com.example.uniclub.dto.response.ConflictEventResponse;
import com.example.uniclub.dto.response.LocationResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventDay;
import com.example.uniclub.entity.Location;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.LocationRepository;
import com.example.uniclub.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepo;
    private final EventRepository eventRepo;

    private LocationResponse toResp(Location l) {
        return LocationResponse.builder()
                .id(l.getLocationId())
                .name(l.getName())
                .address(l.getAddress())
                .capacity(l.getCapacity())
                .build();
    }

    @Override
    public LocationResponse create(LocationCreateRequest req) {
        if (locationRepo.existsByName(req.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Location already exists.");
        Location l = Location.builder()
                .name(req.name())
                .address(req.address())
                .capacity(req.capacity())
                .build();
        return toResp(locationRepo.save(l));
    }

    @Override
    public LocationResponse get(Long id) {
        return locationRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Location not found."));
    }

    @Override
    public Page<LocationResponse> list(Pageable pageable) {
        return locationRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public void delete(Long id) {
        if (!locationRepo.existsById(id))
            throw new ApiException(HttpStatus.NOT_FOUND, "Location not found.");
        locationRepo.deleteById(id);
    }

    @Override
    public LocationResponse update(Long id, LocationUpdateRequest req) {

        Location l = locationRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Location not found."));

        // Update name
        if (req.name() != null && !req.name().isBlank()) {
            if (locationRepo.existsByName(req.name()) && !req.name().equalsIgnoreCase(l.getName())) {
                throw new ApiException(HttpStatus.CONFLICT, "Location name already exists.");
            }
            l.setName(req.name());
        }

        // Update address
        if (req.address() != null) {
            l.setAddress(req.address());
        }

        // Update capacity
        if (req.capacity() != null) {
            l.setCapacity(req.capacity());
        }

        return toResp(locationRepo.save(l));
    }

    @Override
    public List<ConflictEventResponse> checkConflict(Long locationId, LocalDate date,
                                                     LocalTime start, LocalTime end) {

        List<Event> events = eventRepo.findConflictedEvents(locationId, date, start, end);

        List<ConflictEventResponse> result = new ArrayList<>();

        for (Event e : events) {
            for (EventDay d : e.getDays()) {
                if (d.getDate().equals(date) &&
                        d.getStartTime().isBefore(end) &&
                        d.getEndTime().isAfter(start)) {
                    result.add(toConflictResponse(e, d));
                }
            }
        }

        return result;
    }

    public ConflictEventResponse toConflictResponse(Event e, EventDay d) {
        return new ConflictEventResponse(
                e.getEventId(),
                e.getName(),
                d.getDate(),
                d.getStartTime(),
                d.getEndTime(),
                e.getLocation() != null ? e.getLocation().getName() : null
        );
    }

}
