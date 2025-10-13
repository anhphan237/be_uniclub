package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Location;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final ClubRepository clubRepo; // ✅ thêm dòng này

    private EventResponse toResp(Event e) {
        return EventResponse.builder()
                .id(e.getEventId())
                .clubId(e.getClub() != null ? e.getClub().getClubId() : null)
                .name(e.getName())
                .description(e.getDescription())
                .type(e.getType())
                .date(e.getDate())
                .time(e.getTime())
                .status(e.getStatus())
                .checkInCode(e.getCheckInCode())
                .locationId(e.getLocation() != null ? e.getLocation().getLocationId() : null)
                .build();
    }

    @Override
    public EventResponse create(EventCreateRequest req) {
        String randomCode = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Event e = Event.builder()
                .club(Club.builder().clubId(req.clubId()).build())
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .date(req.date())
                .time(req.time())
                .status(EventStatusEnum.PENDING)
                .checkInCode(randomCode)
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
    public EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status) {
        var user = principal.getUser();

        if (!"UNIVERSITY_STAFF".equals(user.getRole().getRoleName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ UNIVERSITY_STAFF mới được duyệt sự kiện.");
        }

        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event không tồn tại"));

        if (status == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ.");
        }

        event.setStatus(status);
        eventRepo.save(event);

        return toResp(event);
    }

    @Override
    public EventResponse findByCheckInCode(String code) {
        Event event = eventRepo.findByCheckInCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mã check-in không hợp lệ"));
        return toResp(event);
    }

    @Override
    public void delete(Long id) {
        if (!eventRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event không tồn tại");
        }
        eventRepo.deleteById(id);
    }

    // ✅ Hàm mới: Lấy danh sách Event theo ClubId
    @Override
    public List<EventResponse> getByClubId(Long clubId) {
        var club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club không tồn tại"));

        return eventRepo.findByClub_ClubId(clubId)
                .stream()
                .map(this::toResp)
                .toList();
    }
}
