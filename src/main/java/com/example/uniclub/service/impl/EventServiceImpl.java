package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    // Giữ các field duplicate theo context hiện có của bạn
    private final EventRepository eventRepository;
    private final EventStaffRepository eventStaffRepository;

    // =========================================================
    // 🔹 MAPPING ENTITY → RESPONSE
    // =========================================================
    private EventResponse toResp(Event event) {
        return EventResponse.builder()
                .id(event.getEventId())
                .name(event.getName())
                .description(event.getDescription())
                .type(event.getType())
                .date(event.getDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .checkInCode(event.getCheckInCode())
                .locationName(event.getLocation() != null ? event.getLocation().getName() : null)
                .maxCheckInCount(event.getMaxCheckInCount())
                .currentCheckInCount(event.getCurrentCheckInCount())
                .hostClub(new EventResponse.SimpleClub(
                        event.getHostClub().getClubId(),
                        event.getHostClub().getName(),
                        EventCoHostStatusEnum.APPROVED
                ))
                .coHostedClubs(
                        event.getCoHostRelations() == null ? List.of() :
                                event.getCoHostRelations().stream()
                                        .map(rel -> new EventResponse.SimpleClub(
                                                rel.getClub().getClubId(),
                                                rel.getClub().getName(),
                                                rel.getStatus()
                                        ))
                                        .toList()
                )
                .budgetPoints(event.getBudgetPoints())
                .build();
    }


    // =========================================================
    // 🔹 TẠO SỰ KIỆN (HOST CLUB gửi → chờ Co-host phản hồi)
    // =========================================================
    @Override
    @Transactional
    public EventResponse create(EventCreateRequest req) {

        // 🔹 0) Kiểm tra ngày & giờ không ở quá khứ
        LocalDate today = LocalDate.now();
        LocalDate eventDate = req.date();

        if (eventDate.isBefore(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ngày sự kiện không được ở quá khứ.");
        }

        if (eventDate.isEqual(today)) {
            LocalTime now = LocalTime.now();
            if (req.startTime() != null && req.startTime().isBefore(now)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu không được ở quá khứ.");
            }
            if (req.endTime() != null && req.endTime().isBefore(now)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc không được ở quá khứ.");
            }
        }

        if (req.startTime() != null && req.endTime() != null && req.endTime().isBefore(req.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        // 🔹 1) Location tồn tại
        Location location = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Địa điểm không tồn tại"));

        // 🔹 2) Kiểm tra sức chứa
        if (req.maxCheckInCount() != null && req.maxCheckInCount() > location.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    String.format("Địa điểm '%s' chỉ chứa tối đa %d người.", location.getName(), location.getCapacity()));
        }

        // 🔹 3) Host Club tồn tại
        Club hostClub = clubRepo.findById(req.hostClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Host club không tồn tại"));

        // 🔹 4) Co-host list (có thể rỗng)
        List<Club> coHostClubs = (req.coHostClubIds() != null && !req.coHostClubIds().isEmpty())
                ? clubRepo.findAllById(req.coHostClubIds())
                : List.of();

        // 🔹 5) Ngân sách
        if (req.budgetPoints() == null || req.budgetPoints() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng nhập ngân sách (budgetPoints) hợp lệ (>0).");
        }

        // 🔹 6) Mã check-in ngẫu nhiên
        String randomCode = "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 🔹 7) Xác định trạng thái ban đầu
        EventStatusEnum initialStatus = coHostClubs.isEmpty()
                ? EventStatusEnum.WAITING_UNISTAFF_APPROVAL   // nếu chỉ có 1 CLB tổ chức
                : EventStatusEnum.WAITING_COCLUB_APPROVAL;    // nếu có co-host

        // 🔹 8) Tạo event
        Event event = Event.builder()
                .hostClub(hostClub)
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .date(req.date())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .location(location)
                .status(initialStatus)
                .checkInCode(randomCode)
                .maxCheckInCount(req.maxCheckInCount())
                .currentCheckInCount(0)
                .commitPointCost(req.commitPointCost())
                .rewardMultiplierCap(2)
                .budgetPoints(req.budgetPoints())
                .build();

        // 🔹 9) Nếu có co-host → tạo quan hệ chờ phê duyệt
        if (!coHostClubs.isEmpty()) {
            List<EventCoClub> coHostRelations = coHostClubs.stream()
                    .map(club -> EventCoClub.builder()
                            .event(event)
                            .club(club)
                            .status(EventCoHostStatusEnum.PENDING)
                            .build())
                    .toList();
            event.setCoHostRelations(coHostRelations);
        }

        eventRepo.save(event);

        // 🔹 10) Gửi thông báo tùy theo loại sự kiện
        if (coHostClubs.isEmpty()) {
            // Không có co-host → gửi thẳng cho UniStaff duyệt
            notificationService.notifyUniStaffReadyForReview(event);
        } else {
            // Có co-host → gửi lời mời đến từng co-host và báo UniStaff chờ
            for (Club co : coHostClubs) {
                notificationService.notifyCoHostInvite(co, event);
            }
            notificationService.notifyUniStaffWaiting(event);
        }

        return toResp(event);
    }


    // =========================================================
    // 🔹 CO-HOST PHẢN HỒI (ACCEPT / REJECT)
    // =========================================================
    @Override
    @Transactional
    public String respondCoHost(Long eventId, CustomUserDetails principal, boolean accepted) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = userRepo.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Tìm CLB của user có trong danh sách co-host của event
        Club coClub = user.getMemberships().stream()
                .map(Membership::getClub)
                .filter(c -> event.getCoHostedClubs().contains(c))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Bạn không phải CLB được mời co-host"));

        EventCoClub relation = event.getCoHostRelations().stream()
                .filter(rel -> rel.getClub().equals(coClub))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy record co-host"));

        relation.setStatus(accepted ? EventCoHostStatusEnum.APPROVED : EventCoHostStatusEnum.REJECTED);
        relation.setRespondedAt(LocalDateTime.now());
        eventRepo.save(event); // cascade quan hệ

        boolean anyReject = event.getCoHostRelations().stream()
                .anyMatch(r -> r.getStatus() == EventCoHostStatusEnum.REJECTED);
        boolean allApproved = event.getCoHostRelations().stream()
                .allMatch(r -> r.getStatus() == EventCoHostStatusEnum.APPROVED);

        if (anyReject) {
            event.setStatus(EventStatusEnum.REJECTED);
            eventRepo.save(event);
            notificationService.notifyHostEventRejectedByCoHost(event, coClub);
            return "❌ Co-host '" + coClub.getName() + "' đã từ chối. Sự kiện bị từ chối bởi co-host.";
        }

        if (allApproved) {
            event.setStatus(EventStatusEnum.WAITING_UNISTAFF_APPROVAL);
            eventRepo.save(event);
            notificationService.notifyUniStaffReadyForReview(event);
            return "✅ Tất cả co-host đã đồng ý. Sự kiện chuyển sang chờ UniStaff duyệt.";
        }

        return accepted
                ? "✅ Co-host '" + coClub.getName() + "' đã đồng ý."
                : "❌ Co-host '" + coClub.getName() + "' đã từ chối.";
    }

    // =========================================================
    // 🔹 UNISTAFF DUYỆT CUỐI (APPROVE / REJECT)
    // =========================================================
    @Override
    @Transactional
    public String reviewByUniStaff(Long eventId, boolean approve, CustomUserDetails principal, Integer budgetPoints) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        String role = principal.getRoleName();
        if (!"UNIVERSITY_STAFF".equals(role) && !"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only UniStaff/Admin can review events.");
        }

        if (event.getStatus() != EventStatusEnum.WAITING_UNISTAFF_APPROVAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not ready for UniStaff review.");
        }

        if (!approve) {
            event.setStatus(EventStatusEnum.REJECTED);
            eventRepo.save(event);
            notificationService.notifyEventRejected(event);
            return "❌ Event '" + event.getName() + "' rejected by UniStaff.";
        }

        // APPROVE
        if (budgetPoints == null || budgetPoints <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Budget points must be provided and > 0.");
        }

        event.setStatus(EventStatusEnum.APPROVED);
        event.setBudgetPoints(budgetPoints);

        // Tạo ví sự kiện & nạp ngân sách
        Wallet wallet = event.getWallet();
        if (wallet == null) {
            wallet = new Wallet();
            wallet.setOwnerType(WalletOwnerTypeEnum.EVENT);
            wallet.setBalancePoints(0L);
            wallet.setEvent(event);
        }
        wallet.setBalancePoints(wallet.getBalancePoints() + budgetPoints);
        walletRepository.save(wallet);
        event.setWallet(wallet);

        eventRepo.save(event);
        notificationService.notifyEventApproved(event);

        return "✅ Event '" + event.getName() + "' approved by UniStaff.";
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
    // 🔹 CẬP NHẬT TRẠNG THÁI (Legacy Approve/Reject trực tiếp)
    //   — vẫn giữ cho backward compatibility (Admin/UniStaff)
    // =========================================================
    @Override
    @Transactional
    public EventResponse updateStatus(CustomUserDetails principal, Long id, EventStatusEnum status, Integer budgetPoints) {
        var user = principal.getUser();
        String roleName = user.getRole().getRoleName();
        if (!"UNIVERSITY_STAFF".equals(roleName) && !"ADMIN".equals(roleName)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only University Staff or Admin can approve events.");
        }

        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // Chặn approve khi chưa qua WAITING_UNISTAFF_APPROVAL
        if (status == EventStatusEnum.APPROVED &&
                event.getStatus() != EventStatusEnum.WAITING_UNISTAFF_APPROVAL) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event must be WAITING_UNISTAFF_APPROVAL before APPROVED.");
        }

        event.setStatus(status);

        if (status == EventStatusEnum.APPROVED) {
            if (budgetPoints == null || budgetPoints <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Budget points must be provided when approving.");
            }
            // tạo ví nếu chưa có
            if (event.getWallet() == null) {
                Wallet eventWallet = new Wallet();
                eventWallet.setOwnerType(WalletOwnerTypeEnum.EVENT);
                eventWallet.setBalancePoints(0L);
                eventWallet.setEvent(event);
                walletRepository.save(eventWallet);
                event.setWallet(eventWallet);
            }
            // nạp ngân sách
            event.setBudgetPoints(budgetPoints);
            Wallet wallet = event.getWallet();
            wallet.setBalancePoints(wallet.getBalancePoints() + budgetPoints);
            walletRepository.save(wallet);
        }

        eventRepo.save(event);

        // thông báo kết quả cho host
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
    // 🔹 DANH SÁCH EVENT CỦA MỘT CLB (host hoặc co-host)
    // =========================================================
    @Override
    public List<EventResponse> getByClubId(Long clubId) {
        clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        List<Event> events = eventRepo.findByClubParticipation(clubId);

        return events.stream()
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
    // 🔹 API MỞ RỘNG
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
                .name(original.getName() + " (Next Term)")
                .description(original.getDescription())
                .date(original.getDate() != null ? original.getDate().plusMonths(6) : null)
                .startTime(original.getStartTime())
                .endTime(original.getEndTime())
                .type(original.getType())
                .location(original.getLocation())
                .status(EventStatusEnum.WAITING_COCLUB_APPROVAL) // clone mở lại vòng co-host
                .checkInCode("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maxCheckInCount(original.getMaxCheckInCount())
                .currentCheckInCount(0)
                .commitPointCost(original.getCommitPointCost())
                .rewardMultiplierCap(original.getRewardMultiplierCap())
                .budgetPoints(original.getBudgetPoints())
                .build();

        List<EventCoClub> clonedRelations = original.getCoHostRelations() != null
                ? original.getCoHostRelations().stream()
                .map(rel -> EventCoClub.builder()
                        .event(clone)
                        .club(rel.getClub())
                        .status(EventCoHostStatusEnum.PENDING)
                        .build())
                .toList()
                : List.of();

        clone.setCoHostRelations(clonedRelations);
        eventRepo.save(clone);
        return toResp(clone);
    }

    @Override
    public Page<EventResponse> filter(String name, LocalDate date, EventStatusEnum status, Pageable pageable) {
        name = (name == null) ? "" : name;

        Page<Event> page;
        if (date != null && status != null) {
            page = eventRepo.findByNameContainingIgnoreCaseAndDateAndStatus(name, date, status, pageable);
        } else if (status != null) {
            page = eventRepo.findByNameContainingIgnoreCaseAndStatus(name, status, pageable);
        } else {
            page = eventRepo.findByNameContainingIgnoreCase(name, pageable);
        }
        return page.map(this::toResponseShort);
    }

    private EventResponse toResponseShort(Event e) {
        return EventResponse.builder()
                .id(e.getEventId())
                .name(e.getName())
                .date(e.getDate())
                .status(e.getStatus())
                .budgetPoints(e.getBudgetPoints())
                .build();
    }

    @Override
    @Transactional
    public EventResponse assignStaff(CustomUserDetails principal, Long eventId, Long membershipId, String duty) {
        var user = principal.getUser();

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // chỉ Leader/Vice của host club
        Membership actorMembership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (!(actorMembership.getClubRole() == ClubRoleEnum.LEADER ||
                actorMembership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Leader or Vice Leader can assign staff.");
        }

        // check target
        Membership target = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Target member not found"));
        if (!target.getClub().getClubId().equals(event.getHostClub().getClubId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Member does not belong to this club.");
        }

        // duplicate
        if (eventStaffRepo.existsByEvent_EventIdAndMembership_MembershipId(eventId, membershipId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This member is already assigned to the event.");
        }

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

        String roleName = user.getRole().getRoleName();
        boolean isPrivileged =
                roleName.equals("ADMIN") ||
                        roleName.equals("UNIVERSITY_STAFF");

        if (!isPrivileged) {
            Membership membership = membershipRepo
                    .findByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));
            if (!(membership.getClubRole() == ClubRoleEnum.LEADER ||
                    membership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to view event staff list.");
            }
        }

        List<EventStaff> staffs = eventStaffRepo.findByEvent_EventIdOrderByIdAsc(eventId);
        return staffs.stream()
                .map(EventStaff::getMembership)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventStaffResponse> getEventStaffList(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        List<EventStaff> staffs = eventStaffRepository.findByEvent_EventId(eventId);

        return staffs.stream().map(staff -> EventStaffResponse.builder()
                .id(staff.getId())
                .eventId(event.getEventId())
                .eventName(event.getName())
                .membershipId(staff.getMembership().getMembershipId())
                .memberName(staff.getMembership().getUser() != null
                        ? staff.getMembership().getUser().getFullName() : null)
                .duty(staff.getDuty())
                .state(staff.getState())
                .assignedAt(staff.getAssignedAt())
                .unassignedAt(staff.getUnassignedAt())
                .build()
        ).toList();
    }

    @Override
    public List<EventResponse> getCoHostedEvents(Long clubId) {
        clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found"));

        List<Event> events = eventRepo.findCoHostedEvents(clubId);

        return events.stream()
                .map(this::toResp)
                .toList();
    }

    @Override
    public Event getEntity(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    // =========================================================
    // 🔹 BACKWARD COMPATIBILITY (DEPRECATED): chĩa về flow mới
    // =========================================================
    @Override
    @Transactional
    public String acceptCohost(Long eventId, CustomUserDetails principal) {
        // dùng respondCoHost(..., true)
        return respondCoHost(eventId, principal, true);
    }

    @Override
    @Transactional
    public String rejectCohost(Long eventId, CustomUserDetails principal) {
        // dùng respondCoHost(..., false)
        return respondCoHost(eventId, principal, false);
    }

    @Override
    @Transactional
    public String submitEventToUniStaff(Long eventId, CustomUserDetails principal) {
        // Với flow mới, chỉ valid khi tất cả co-host đã APPROVED
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = userRepo.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Club hostClub = event.getHostClub();
        boolean isHostLeader = user.getMemberships().stream()
                .anyMatch(m -> m.getClub().getClubId().equals(hostClub.getClubId())
                        && (m.getClubRole() == ClubRoleEnum.LEADER || m.getClubRole() == ClubRoleEnum.VICE_LEADER));

        if (!isHostLeader)
            throw new ApiException(HttpStatus.FORBIDDEN, "Only Host Club's Leader or Vice Leader can submit event.");

        if (event.getCoHostRelations() != null && !event.getCoHostRelations().isEmpty()) {
            boolean allApproved = event.getCoHostRelations().stream()
                    .allMatch(r -> r.getStatus() == EventCoHostStatusEnum.APPROVED);
            if (!allApproved) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "All co-host clubs must accept before submission.");
            }
        }

        event.setStatus(EventStatusEnum.WAITING_UNISTAFF_APPROVAL);
        eventRepo.save(event);
        notificationService.notifyUniStaffReadyForReview(event);

        return "📤 Event '" + event.getName() + "' submitted to UniStaff (WAITING_UNISTAFF_APPROVAL).";
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getActiveEvents() {
        LocalDate today = LocalDate.now();
        return eventRepo.findActiveEvents(EventStatusEnum.APPROVED, today)
                .stream()
                .map(this::toResp)
                .toList();
    }
    @Override
    @Transactional
    public String finishEvent(Long eventId, CustomUserDetails principal) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // 🔒 Chỉ cho phép Leader hoặc UniStaff kết thúc
        var user = principal.getUser();
        boolean isUniStaff = user.getRole().getRoleName().equals("UNIVERSITY_STAFF");
        boolean isLeader = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndClubRoleIn(
                user.getUserId(), event.getHostClub().getClubId(),
                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
        );

        if (!isUniStaff && !isLeader)
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to mark this event as finished.");

        // ⚙️ Cập nhật trạng thái
        if (event.getStatus() != EventStatusEnum.APPROVED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved events can be marked as finished.");

        event.setStatus(EventStatusEnum.FINISHED);
        eventRepo.save(event);

        notificationService.notifyUniStaffReadyForReview(event);

        return "🟡 Event '" + event.getName() + "' has been marked as FINISHED and is now ready for settlement.";
    }
    @Override
    @Transactional
    public String settleEvent(Long eventId, CustomUserDetails principal) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // 🔒 Chỉ UniStaff mới được settle
        var user = principal.getUser();
        if (!"UNIVERSITY_STAFF".equals(user.getRole().getRoleName()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Only UniStaff can settle events.");

        if (event.getStatus() != EventStatusEnum.FINISHED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only finished events can be settled.");

        // 🪙 Tính toán thưởng & hoàn ví (giản lược)
        Wallet wallet = event.getWallet();
        if (wallet == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event wallet not found.");

        long totalPoints = wallet.getBalancePoints();
        wallet.setBalancePoints(0L); // Giả định toàn bộ điểm được chi ra
        walletRepository.save(wallet);

        event.setStatus(EventStatusEnum.SETTLED);
        eventRepo.save(event);

        notificationService.notifyEventApproved(event);

        return String.format("✅ Event '%s' has been SETTLED (total %.0f points processed).",
                event.getName(), (double) totalPoints);
    }
    @Override
    @Transactional
    public String markEventCompleted(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.SETTLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event must be SETTLED before marking as COMPLETED");
        }

        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);
        return "📦 Event '" + event.getName() + "' has been archived (COMPLETED).";
    }

}
