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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final ClubRepository clubRepo;
    private final LocationRepository locationRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final NotificationService notificationService;
    private final RewardService rewardService;
    private final WalletRepository walletRepository;
    private final EventRegistrationRepository regRepo;
    private final EventStaffRepository eventStaffRepo;
    // =========================================================
    // 🔹 MAPPING ENTITY → RESPONSE
    // =========================================================
    private EventResponse toResp(Event e) {
        return EventResponse.builder()
                .id(e.getEventId())
                .name(e.getName())
                .description(e.getDescription())
                .type(e.getType())
                .date(e.getDate())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .status(e.getStatus())
                .checkInCode(e.getCheckInCode())
                .locationName(e.getLocation() != null ? e.getLocation().getName() : null)
                .maxCheckInCount(e.getMaxCheckInCount())
                .currentCheckInCount(e.getCurrentCheckInCount())
                .hostClub(e.getHostClub() != null
                        ? new EventResponse.SimpleClub(e.getHostClub().getClubId(), e.getHostClub().getName())
                        : null)
                .coHostedClubs(e.getCoHostedClubs() != null
                        ? e.getCoHostedClubs().stream()
                        .map(c -> new EventResponse.SimpleClub(c.getClubId(), c.getName()))
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }

    // =========================================================
    // 🔹 TẠO SỰ KIỆN (CLUB LEADER GỬI YÊU CẦU)
    // =========================================================
    @Override
    public EventResponse create(EventCreateRequest req) {

        // 1️⃣ Kiểm tra location hợp lệ
        Location location = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Địa điểm không tồn tại"));

        // 2️⃣ Kiểm tra sức chứa location
        if (req.maxCheckInCount() != null && req.maxCheckInCount() > location.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, String.format(
                    "Địa điểm '%s' chỉ chứa tối đa %d người. Vui lòng giảm số lượng hoặc chọn địa điểm khác.",
                    location.getName(), location.getCapacity()
            ));
        }

        // 3️⃣ Lấy CLB chủ trì
        Club hostClub = clubRepo.findById(req.hostClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Host club không tồn tại"));

        // 4️⃣ Lấy danh sách CLB đồng tổ chức (nếu có)
        List<Club> coHosts = (req.coHostClubIds() != null && !req.coHostClubIds().isEmpty())
                ? clubRepo.findAllById(req.coHostClubIds())
                : List.of();

        // 5️⃣ Xác định điểm cam kết mặc định
        int finalCommitCost = (req.commitPointCost() != null && req.commitPointCost() > 0)
                ? req.commitPointCost()
                : 100;

        // 6️⃣ Tạo mã sự kiện ngẫu nhiên
        String randomCode = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 7️⃣ Khởi tạo đối tượng Event
        Event event = Event.builder()
                .hostClub(hostClub)
                .coHostedClubs(coHosts)
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .date(req.date())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .location(location)
                .status(EventStatusEnum.PENDING)
                .checkInCode(randomCode)
                .maxCheckInCount(req.maxCheckInCount())
                .currentCheckInCount(0)
                .commitPointCost(finalCommitCost)
                .rewardMultiplierCap(2)
                .build();

        eventRepo.save(event);

        // 8️⃣ Gửi thông báo cho UniStaff
        String staffEmail = "uniclub.contacts@gmail.com";
        notificationService.sendEventApprovalRequest(
                staffEmail,
                hostClub.getName(),
                req.name()
        );

        return toResp(event);
    }

    // =========================================================
    // 🔹 LẤY CHI TIẾT SỰ KIỆN
    // =========================================================
    @Override
    public EventResponse get(Long id) {
        return eventRepo.findById(id)
                .map(this::toResp)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    // =========================================================
    // 🔹 DANH SÁCH SỰ KIỆN (PAGINATION)
    // =========================================================
    @Override
    public Page<EventResponse> list(Pageable pageable) {
        return eventRepo.findAll(pageable).map(this::toResp);
    }

    // =========================================================
    // 🔹 CẬP NHẬT TRẠNG THÁI (DUYỆT / TỪ CHỐI)
    // =========================================================
    @Override
    @Transactional
    public EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status) {
        var user = principal.getUser();

        // Chỉ cho phép University Staff hoặc Admin duyệt
        String roleName = user.getRole().getRoleName();
        if (!"UNIVERSITY_STAFF".equals(roleName) && !"ADMIN".equals(roleName)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only University Staff or Admin can approve events.");
        }

        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        event.setStatus(status);

        // Khi APPROVED → tạo ví Event + cấp điểm
        if (status == EventStatusEnum.APPROVED) {
            if (event.getWallet() == null) {
                Wallet eventWallet = new Wallet();
                eventWallet.setOwnerType(WalletOwnerTypeEnum.EVENT);
                eventWallet.setBalancePoints(0);
//                eventWallet.setClub(event.getHostClub());
                walletRepository.save(eventWallet);
                event.setWallet(eventWallet);
            }

            int capacity = event.getMaxCheckInCount() != null ? event.getMaxCheckInCount() : 0;
            if (capacity <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Cannot approve event without setting expected participant count.");
            }

            int basePointPerMember = 100;
            int totalGrant = capacity * basePointPerMember;

            Wallet wallet = event.getWallet();
            wallet.setBalancePoints(wallet.getBalancePoints() + totalGrant);
            walletRepository.save(wallet);

            System.out.printf("🎓 University granted %d points to Event ID %d (%s)%n",
                    totalGrant, event.getEventId(), event.getName());
        }

        eventRepo.save(event);

        // Gửi email thông báo kết quả duyệt
        String contactEmail = resolveClubContactEmail(event.getHostClub().getClubId())
                .orElseGet(() -> event.getHostClub().getCreatedBy() != null
                        ? event.getHostClub().getCreatedBy().getEmail()
                        : null);

        boolean approved = status == EventStatusEnum.APPROVED;
        if (contactEmail != null && !contactEmail.isBlank()) {
            notificationService.sendEventApprovalResult(contactEmail, event.getName(), approved);
        }

        return toResp(event);
    }

    // =========================================================
    // 🔹 TÌM KIẾM SỰ KIỆN QUA MÃ CHECK-IN
    // =========================================================
    @Override
    public EventResponse findByCheckInCode(String code) {
        Event event = eventRepo.findByCheckInCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code"));
        return toResp(event);
    }

    // =========================================================
    // 🔹 XÓA SỰ KIỆN
    // =========================================================
    @Override
    public void delete(Long id) {
        if (!eventRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        }
        eventRepo.deleteById(id);
    }

    // =========================================================
    // 🔹 DANH SÁCH EVENT CỦA MỘT CLB
    // =========================================================
    @Override
    public List<EventResponse> getByClubId(Long clubId) {
        clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));
        return eventRepo.findByHostClub_ClubId(clubId)
                .stream()
                .map(this::toResp)
                .toList();

    }

    // =========================================================
    // 🔹 LẤY EMAIL LIÊN HỆ CỦA CLB
    // =========================================================
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

    // =========================================================
    // 🔹 CÁC API MỞ RỘNG
    // =========================================================
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
                .hostClub(original.getHostClub())
                .coHostedClubs(original.getCoHostedClubs())
                .name(original.getName() + " (Next Term)")
                .description(original.getDescription())
                .date(original.getDate() != null ? original.getDate().plusMonths(6) : null)
                .startTime(original.getStartTime())
                .endTime(original.getEndTime())
                .type(original.getType())
                .status(EventStatusEnum.PENDING)
                .checkInCode("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maxCheckInCount(original.getMaxCheckInCount())
                .currentCheckInCount(0)
                .build();

        eventRepo.save(clone);
        return toResp(clone);
    }

    @Override
    public Page<EventResponse> filter(String name, LocalDate date, EventStatusEnum status, Pageable pageable) {
        name = (name == null) ? "" : name; // tránh null pointer

        Page<Event> page;

        if (date != null && status != null) {
            page = eventRepo.findByNameContainingIgnoreCaseAndDateAndStatus(name, date, status, pageable);
        } else if (status != null) {
            page = eventRepo.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
        } else {
            page = eventRepo.findByNameContainingIgnoreCase(name, pageable);
        }

        return page.map(this::toResponse);
    }

    private EventResponse toResponse(Event e) {
        return EventResponse.builder()
                .id(e.getEventId())
                .name(e.getName())
                .date(e.getDate())
                .status(e.getStatus())
                .build();
    }
    @Override
    @Transactional
    public EventResponse assignStaff(CustomUserDetails principal, Long eventId, Long membershipId, String duty) {
        var user = principal.getUser();

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // 1️⃣ Kiểm tra quyền: chỉ Leader hoặc Vice-Leader của CLB chủ trì
        Membership actorMembership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (!(actorMembership.getClubRole() == ClubRoleEnum.LEADER ||
                actorMembership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can assign staff.");
        }

        // 2️⃣ Kiểm tra thành viên được gán có tồn tại & cùng CLB
        Membership target = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target member not found"));
        if (!target.getClub().getClubId().equals(event.getHostClub().getClubId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Member does not belong to this club.");
        }

        // 3️⃣ Kiểm tra trùng lặp
        if (eventStaffRepo.existsByEvent_EventIdAndMembership_MembershipId(eventId, membershipId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This member is already assigned to the event.");
        }

        // 4️⃣ Gán staff mới
        EventStaff staff = EventStaff.builder()
                .event(event)
                .membership(target)
                .duty(duty)
                .build();
        eventStaffRepo.save(staff);

        return toResp(event);
    }
    @Override
    public List<Membership> getEventStaffs(CustomUserDetails principal, Long eventId) {
        var user = principal.getUser();

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // 1️⃣ Kiểm tra quyền: Leader/Vice/UniStaff/Admin mới được xem danh sách staff
        String roleName = user.getRole().getRoleName();
        boolean isPrivileged =
                roleName.equals("ADMIN") ||
                        roleName.equals("UNIVERSITY_STAFF");

        if (!isPrivileged) {
            // Nếu là member trong club, cần là leader hoặc vice
            Membership membership = membershipRepo
                    .findByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

            if (!(membership.getClubRole() == ClubRoleEnum.LEADER ||
                    membership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to view event staff list.");
            }
        }

        // 2️⃣ Lấy danh sách staff
        List<EventStaff> staffs = eventStaffRepo.findByEvent_EventIdOrderByIdAsc(eventId);
        return staffs.stream()
                .map(EventStaff::getMembership)
                .collect(Collectors.toList());
    }

}
