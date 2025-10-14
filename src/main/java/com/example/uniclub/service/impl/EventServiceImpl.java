package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Location;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EventService;
import com.example.uniclub.service.NotificationService;
import com.example.uniclub.service.RewardService;
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
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final RewardService rewardService;

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
                .maxCheckInCount(req.maxCheckInCount())
                .currentCheckInCount(0)
                .build();

        eventRepo.save(e);

        // 📨 Gửi thông báo đến staff để duyệt
        var club = clubRepo.findById(req.clubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        String staffEmail = "uniclub.contacts@gmail.com"; // hoặc query staff thực tế
        notificationService.sendEventApprovalRequest(staffEmail, club.getName(), req.name());

        return toResp(e);
    }

    @Override
    public EventResponse get(Long id) {
        return eventRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Override
    public Page<EventResponse> list(Pageable pageable) {
        return eventRepo.findAll(pageable).map(this::toResp);
    }

    @Override
    public EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status) {
        var user = principal.getUser();

        if (!"UNIVERSITY_STAFF".equals(user.getRole().getRoleName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only University Staff can approve events.");
        }

        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        event.setStatus(status);
        eventRepo.save(event);

        // 📨 Gửi thông báo kết quả duyệt cho leader
        var leaderEmail = event.getClub().getLeader().getEmail();
        boolean approved = status == EventStatusEnum.APPROVED;
        notificationService.sendEventApprovalResult(leaderEmail, event.getName(), approved);

        return toResp(event);
    }

    @Override
    public EventResponse findByCheckInCode(String code) {
        Event event = eventRepo.findByCheckInCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code"));
        return toResp(event);
    }

    @Override
    public void delete(Long id) {
        if (!eventRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        }
        eventRepo.deleteById(id);
    }

    @Override
    public List<EventResponse> getByClubId(Long clubId) {
        var club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        return eventRepo.findByClub_ClubId(clubId)
                .stream()
                .map(this::toResp)
                .toList();
    }

    // ✅ Khi student check-in event
    public String checkIn(CustomUserDetails principal, String code) {
        Event event = eventRepo.findByCheckInCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Check-in code not found"));

        // kiểm tra giới hạn người tham gia
        if (event.getMaxCheckInCount() != null &&
                event.getCurrentCheckInCount() >= event.getMaxCheckInCount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event already full!");
        }

        // cập nhật số lượng check-in
        event.setCurrentCheckInCount(event.getCurrentCheckInCount() + 1);
        eventRepo.save(event);

        // 🟢 Cộng điểm thưởng
        User user = principal.getUser();
        int rewardPoints = 10; // mỗi sự kiện +10 điểm
        int totalPoints = user.getWallet().getBalancePoints() + rewardPoints;
        user.getWallet().setBalancePoints(totalPoints);
        userRepo.save(user);

        // 📨 Gửi email thưởng điểm
        rewardService.sendCheckInRewardEmail(user.getUserId(), event.getName(), rewardPoints, totalPoints);

        // 🏆 Kiểm tra milestone
        if (totalPoints == 100 || totalPoints == 200 || totalPoints == 500) {
            rewardService.sendMilestoneEmail(user.getUserId(), totalPoints);
        }

        return "✅ Check-in successful! You’ve earned " + rewardPoints + " UniPoints. Total: " + totalPoints;
    }
}
