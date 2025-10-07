package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Location;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;

    private EventResponse toResp(Event e) {
        return EventResponse.builder()
                .id(e.getEventId())
                .clubId(e.getClub() != null ? e.getClub().getClubId() : null)
                .name(e.getName())
                .description(e.getDescription())
                .type(e.getType())
                .date(e.getDate())
                .time(e.getTime())
                .locationId(e.getLocation() != null ? e.getLocation().getLocationId() : null)
                .build();
    }

    @Override
    public EventResponse create(EventCreateRequest req) {
        Event e = Event.builder()
                .club(Club.builder().clubId(req.clubId()).build())
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .date(req.date())
                .time(req.time()) // không cần convert nữa
                .location(req.locationId() == null ? null :
                        Location.builder().locationId(req.locationId()).build())
                .build();

        return toResp(eventRepo.save(e));
    }

    @Override
    public EventResponse get(Long id) {
        return eventRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event không tồn tại"));
    }

    @Override
    public Page<EventResponse> list(Pageable pageable) {
        return eventRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public void delete(Long id) {
        if (!eventRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event không tồn tại");
        }
        eventRepo.deleteById(id);
    }
}
