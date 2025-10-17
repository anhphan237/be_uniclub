package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EventService;
import com.example.uniclub.service.NotificationService;
import com.example.uniclub.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final ClubRepository clubRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final NotificationService notificationService;
    private final RewardService rewardService;
    private final WalletRepository walletRepository;
    private final EventRegistrationRepository regRepo;

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
                .locationName(e.getLocation() != null ? e.getLocation().getName() : null)
                .maxCheckInCount(e.getMaxCheckInCount())
                .currentCheckInCount(e.getCurrentCheckInCount())
                .build();
    }

    @Override
    public EventResponse create(EventCreateRequest req) {
        String randomCode = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 🟢 Lấy giá trị commitPointCost từ request (hoặc dùng mặc định 100)
        int finalCommitCost = (req.commitPointCost() != null && req.commitPointCost() > 0)
                ? req.commitPointCost()
                : 100;

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
                .commitPointCost(finalCommitCost) // ✅ thêm commitPointCost
                .rewardMultiplierCap(3) // ✅ đảm bảo luôn có giá trị mặc định trần nhân thưởng
                .build();

        eventRepo.save(e);

        // 📨 Gửi email cho staff để duyệt
        var club = clubRepo.findById(req.clubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        String staffEmail = "uniclub.contacts@gmail.com";
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
    @Transactional
    public EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status) {
        var user = principal.getUser();

        // ✅ Cho phép cả ADMIN và UNIVERSITY_STAFF duyệt sự kiện
        String roleName = user.getRole().getRoleName();
        if (!"UNIVERSITY_STAFF".equals(roleName) && !"ADMIN".equals(roleName)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only University Staff or Admin can approve events.");
        }

        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        event.setStatus(status);

        // ✅ Khi duyệt APPROVED: tạo ví EVENT và cấp điểm dựa theo số lượng người dự kiến
        if (status == EventStatusEnum.APPROVED) {
            if (event.getWallet() == null) {
                Wallet eventWallet = new Wallet();
                eventWallet.setOwnerType(WalletOwnerTypeEnum.EVENT);
                eventWallet.setBalancePoints(0);
                eventWallet.setClub(event.getClub()); // ⚙️ Gắn Club để tránh lỗi constraint DB
                walletRepository.save(eventWallet);
                event.setWallet(eventWallet);
            }

            // 🧮 Tính số điểm được cấp cho ví Event dựa trên số người dự kiến
            int capacity = event.getMaxCheckInCount() != null ? event.getMaxCheckInCount() : 0;
            if (capacity <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Cannot approve event without setting expected participant count (maxCheckInCount).");
            }

            // 🏫 Mặc định nhà trường cấp 100 điểm / người dự kiến
            int basePointPerMember = 100;
            int totalGrant = capacity * basePointPerMember;

            // ✅ Nạp điểm vào ví Event (do UniStaff cấp)
            Wallet wallet = event.getWallet();
            wallet.setBalancePoints(wallet.getBalancePoints() + totalGrant);
            walletRepository.save(wallet);

            System.out.printf("🎓 University granted %d points to Event ID %d (%s)%n",
                    totalGrant, event.getEventId(), event.getName());
        }

        eventRepo.save(event);

        // 📨 Gửi email thông báo kết quả duyệt cho CLB
        String contactEmail = resolveClubContactEmail(event.getClub().getClubId())
                .orElseGet(() -> event.getClub().getCreatedBy() != null
                        ? event.getClub().getCreatedBy().getEmail()
                        : null);

        boolean approved = status == EventStatusEnum.APPROVED;
        if (contactEmail != null && !contactEmail.isBlank()) {
            notificationService.sendEventApprovalResult(contactEmail, event.getName(), approved);
        }

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
        clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        return eventRepo.findByClub_ClubId(clubId)
                .stream()
                .map(this::toResp)
                .toList();
    }

    // ✅ Lấy email liên hệ của CLB (leader hoặc vice-leader)
    private Optional<String> resolveClubContactEmail(Long clubId) {
        Optional<Membership> leader = membershipRepo
                .findFirstByClub_ClubIdAndClubRoleAndState(
                        clubId, ClubRoleEnum.LEADER, MembershipStateEnum.APPROVED);
        if (leader.isPresent() && leader.get().getUser() != null) {
            String email = leader.get().getUser().getEmail();
            if (email != null && !email.isBlank()) return Optional.of(email);
        }

        return membershipRepo
                .findFirstByClub_ClubIdAndClubRoleAndState(
                        clubId, ClubRoleEnum.VICE_LEADER, MembershipStateEnum.APPROVED)
                .map(m -> m.getUser() != null ? m.getUser().getEmail() : null)
                .filter(email -> email != null && !email.isBlank());
    }

    // ==============================
    // 🔹 NEW EXTENDED METHODS 🔹
    // ==============================

    @Override
    public List<EventResponse> getUpcomingEvents() {
        LocalDate today = LocalDate.now();
        return eventRepo.findByDateAfter(today)
                .stream()
                .filter(e -> e.getStatus() == EventStatusEnum.APPROVED)
                .map(this::toResp)
                .toList();
    }

    @Override
    public List<EventResponse> getMyEvents(CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<Event> events = eventRepo.findEventsByUserId(userId);
        return events.stream().map(this::toResp).toList();
    }

    @Override
    @Transactional
    public EventResponse cloneEvent(Long eventId) {
        Event original = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        Event clone = Event.builder()
                .club(original.getClub())
                .name(original.getName() + " (Next Term)")
                .description(original.getDescription())
                .date(original.getDate() != null ? original.getDate().plusMonths(6) : null)
                .time(original.getTime())
                .type(original.getType())
                .status(EventStatusEnum.PENDING)
                .checkInCode("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maxCheckInCount(original.getMaxCheckInCount())
                .currentCheckInCount(0)
                .build();

        eventRepo.save(clone);
        return toResp(clone);
    }

}
