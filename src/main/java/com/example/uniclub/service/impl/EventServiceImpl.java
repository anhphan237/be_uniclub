package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventBudgetApproveRequest;
import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.request.EventExtendRequest;
import com.example.uniclub.dto.response.EventRegistrationResponse;
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
import com.example.uniclub.util.CsvExportUtil;
import com.example.uniclub.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.uniclub.repository.ProductRepository;

import java.time.format.DateTimeFormatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final ClubRepository clubRepo;
    private final LocationRepository locationRepo;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final NotificationService notificationService;
    private final WalletRepository walletRepo;
    private final EventStaffRepository eventStaffRepo;
    private final EventRegistrationRepository eventRegistrationRepo;
    private final WalletTransactionRepository walletTransactionRepo;
    private final ProductRepository productRepo;

    // =================================================================
    // 🔹 MAPPER
    // =================================================================
    private EventResponse mapToResponse(Event event) {
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
                .commitPointCost(event.getCommitPointCost())
                .budgetPoints(event.getBudgetPoints())
                .locationName(event.getLocation() != null ? event.getLocation().getName() : null)
                .maxCheckInCount(event.getMaxCheckInCount())
                .currentCheckInCount(event.getCurrentCheckInCount())

                .hostClub(new EventResponse.SimpleClub(
                        event.getHostClub().getClubId(),
                        event.getHostClub().getName(),
                        EventCoHostStatusEnum.APPROVED
                ))
                .coHostedClubs(event.getCoHostRelations() == null ? List.of() :
                        event.getCoHostRelations().stream()
                                .map(rel -> new EventResponse.SimpleClub(
                                        rel.getClub().getClubId(),
                                        rel.getClub().getName(),
                                        rel.getStatus()))
                                .toList())
                .build();
    }

    // =================================================================
    // 🔹 TẠO SỰ KIỆN
    // =================================================================
    @Override
    @Transactional
    public EventResponse create(EventCreateRequest req) {
        LocalDate today = LocalDate.now();

        // ✅ 1. Validate date is not in the past
        if (req.date().isBefore(today))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event date cannot be in the past.");

        // ✅ 2. Validate start and end times
        if (req.startTime() != null && req.endTime() != null && req.endTime().isBefore(req.startTime()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time.");

        // ✅ 3. If the event is today, start time must be after current time
        if (req.date().isEqual(today) && req.startTime() != null && req.startTime().isBefore(LocalTime.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start time must be after the current time.");

        // ✅ 4. Validate location
        Location location = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Location not found."));

        if (req.maxCheckInCount() != null && req.maxCheckInCount() > location.getCapacity())
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This location can only accommodate up to " + location.getCapacity() + " people.");

        // ✅ 5. Validate host club
        Club hostClub = clubRepo.findById(req.hostClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Host club not found."));

        // ✅ 6. Validate co-host clubs
        List<Club> coHosts = (req.coHostClubIds() != null && !req.coHostClubIds().isEmpty())
                ? clubRepo.findAllById(req.coHostClubIds())
                : List.of();

        // ✅ 7. Validate budget
        if (req.budgetPoints() == null || req.budgetPoints() <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Budget must be greater than 0.");

        // ✅ 8. Validate commitment points
        if (req.commitPointCost() != null && req.commitPointCost() < 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid commitment points value.");

    // ✅ 9. Tạo sự kiện
        Event event = Event.builder()
                .hostClub(hostClub)
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .date(req.date())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .location(location)
                .status(coHosts.isEmpty() ? EventStatusEnum.PENDING_UNISTAFF : EventStatusEnum.PENDING_COCLUB)
                .checkInCode("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maxCheckInCount(req.maxCheckInCount())
                .commitPointCost(req.commitPointCost())
                .budgetPoints(req.budgetPoints())
                .rewardMultiplierCap(2)
                .build();

        // ✅ 10. Tạo quan hệ Co-host nếu có
        if (!coHosts.isEmpty()) {
            List<EventCoClub> coRelations = coHosts.stream()
                    .map(c -> EventCoClub.builder()
                            .event(event)
                            .club(c)
                            .status(EventCoHostStatusEnum.PENDING)
                            .build())
                    .toList();
            event.setCoHostRelations(coRelations);
        }

        // ✅ 11. Lưu sự kiện
        eventRepo.save(event);

        // ✅ 12. Gửi thông báo
        if (coHosts.isEmpty()) {
            notificationService.notifyUniStaffReadyForReview(event);
        } else {
            coHosts.forEach(c -> notificationService.notifyCoHostInvite(c, event));
            notificationService.notifyUniStaffWaiting(event);
        }

        return mapToResponse(event);
    }


    // =================================================================
    // 🔹 CO-HOST PHẢN HỒI
    // =================================================================
    @Override
    @Transactional
    public String respondCoHost(Long eventId, CustomUserDetails principal, boolean accepted) {
        // 🔹 1. Retrieve the event
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event could not be found."));

        // 🔹 DEBUG: Initial log
        log.info("DEBUG >>> userId={}, role=LEADER, state=ACTIVE", principal.getUser().getUserId());

        Long userId = principal.getUser().getUserId();

        // 🔹 2. Find the membership where user is an ACTIVE LEADER
        Membership leaderMembership = membershipRepo
                .findByUser_UserIdAndClubRoleAndState(
                        userId,
                        ClubRoleEnum.LEADER,
                        MembershipStateEnum.ACTIVE
                )
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a valid leader of any club."));

        // 🔹 DEBUG: Confirm membership
        log.info("DEBUG >>> LeaderMembership found: clubId={}", leaderMembership.getClub().getClubId());

        Club coClub = leaderMembership.getClub();

        // 🔹 DEBUG: List of co-host clubs in this event
        log.info("DEBUG >>> Event {} coHostRelations: {}", eventId,
                event.getCoHostRelations().stream().map(r -> r.getClub().getClubId()).toList());

        // 🔹 3. Check if this club is actually a co-host of the event
        boolean isCoHost = event.getCoHostRelations().stream()
                .anyMatch(r -> Objects.equals(r.getClub().getClubId(), coClub.getClubId()));

        if (!isCoHost) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a co-host of this event.");
        }

        // 🔹 4. Retrieve the specific EventCoClub relation
        EventCoClub relation = event.getCoHostRelations().stream()
                .filter(r -> Objects.equals(r.getClub().getClubId(), coClub.getClubId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Co-host relationship not found."));

        // 🔹 5. Update response status
        relation.setStatus(accepted ? EventCoHostStatusEnum.APPROVED : EventCoHostStatusEnum.REJECTED);
        relation.setRespondedAt(LocalDateTime.now());

        // 🔹 6. Log debug response result
        log.info("CoHostRespond >>> eventId={}, coClub={}, accepted={}", eventId, coClub.getName(), accepted);

        // 🔹 7. Handle event status after response
        long approved = event.getCoHostRelations().stream()
                .filter(r -> r.getStatus() == EventCoHostStatusEnum.APPROVED).count();
        long rejected = event.getCoHostRelations().stream()
                .filter(r -> r.getStatus() == EventCoHostStatusEnum.REJECTED).count();
        long total = event.getCoHostRelations().size();

        if (total == 1 && rejected == 1) {
            event.setStatus(EventStatusEnum.REJECTED);
            eventRepo.save(event);
            notificationService.notifyHostEventRejectedByCoHost(event, coClub);
            return "The only co-host '" + coClub.getName() + "' has rejected. The event has been cancelled.";
        }

        if (approved > 0 && (approved + rejected == total)) {
            event.setStatus(EventStatusEnum.PENDING_UNISTAFF);

            // ✅ Keep orphanRemoval; only remove unapproved co-hosts instead of replacing the list
            event.getCoHostRelations().removeIf(r -> r.getStatus() != EventCoHostStatusEnum.APPROVED);

            eventRepo.save(event);
            notificationService.notifyUniStaffReadyForReview(event);
            return "Some co-hosts have approved. The event has been submitted for UniStaff review.";
        }

        if (approved + rejected < total)
            return accepted
                    ? "Co-host '" + coClub.getName() + "' has approved. Waiting for other co-hosts to respond."
                    : "Co-host '" + coClub.getName() + "' has rejected. Waiting for other co-hosts to respond.";

        if (approved == total) {
            event.setStatus(EventStatusEnum.PENDING_UNISTAFF);
            eventRepo.save(event);
            notificationService.notifyUniStaffReadyForReview(event);
            return "All co-hosts have approved. The event is now pending UniStaff review.";
        }

        return "Co-host response has been recorded.";
    }



    // =================================================================
    // 🔹 KẾT THÚC SỰ KIỆN
    // =================================================================
    @Override
    @Transactional
    public String finishEvent(Long eventId, CustomUserDetails principal) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        var user = principal.getUser();
        boolean isUniStaff = user.getRole().getRoleName().equals("UNIVERSITY_STAFF");
        boolean isLeader = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndClubRoleIn(
                user.getUserId(),
                event.getHostClub().getClubId(),
                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
        );

        if (!isUniStaff && !isLeader)
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to finish this event.");

        if (!List.of(EventStatusEnum.APPROVED, EventStatusEnum.ONGOING).contains(event.getStatus()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved or ongoing events can be finished.");

        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);
        return "🟣 The event '" + event.getName() + "' has been marked as completed.";
    }

    // =================================================================
// 🔹 LOOKUP & FILTER
// =================================================================
    @Override
    public EventResponse get(Long id) {
        return eventRepo.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));
    }


    @Override
    public Page<EventResponse> list(Pageable pageable) {
        return eventRepo.findAll(pageable).map(this::mapToResponse);
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
        return page.map(this::mapToResponse);
    }

    @Override
    public EventResponse findByCheckInCode(String code) {
        Event event = eventRepo.findByCheckInCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code."));
        return mapToResponse(event);
    }

    @Override
    public List<EventResponse> getByClubId(Long clubId) {
        List<Event> events = eventRepo.findByClubParticipation(clubId);
        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }



    @Override
    public List<EventResponse> getUpcomingEvents() {
        LocalDate today = LocalDate.now();
        return eventRepo.findByDateAfter(today).stream()
                .filter(e -> List.of(EventStatusEnum.APPROVED, EventStatusEnum.ONGOING).contains(e.getStatus()))
                .map(this::mapToResponse).toList();
    }

    @Override
    public List<EventResponse> getMyEvents(CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        return eventRepo.findEventsByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<EventResponse> getActiveEvents() {
        LocalDate today = LocalDate.now();
        return eventRepo.findActiveEvents(EventStatusEnum.APPROVED, today).stream()
                .filter(e -> List.of(EventStatusEnum.APPROVED, EventStatusEnum.ONGOING).contains(e.getStatus()))
                .map(this::mapToResponse).toList();
    }

    @Override
    public List<EventResponse> getCoHostedEvents(Long clubId) {
        return eventRepo.findCoHostedEvents(clubId)
                .stream().map(this::mapToResponse).toList();
    }

    // =================================================================
    // 🔹 STAFF
    // =================================================================
    @Override
    @Transactional
    public EventResponse assignStaff(CustomUserDetails principal, Long eventId, Long membershipId, String duty) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        Membership actor = membershipRepo.findByUser_UserIdAndClub_ClubId(
                principal.getUser().getUserId(),
                event.getHostClub().getClubId()
        ).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this club."));

        if (!List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER).contains(actor.getClubRole()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the Leader or Vice Leader can assign staff members.");

        Membership target = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found."));
        if (!target.getClub().equals(event.getHostClub()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "The member does not belong to the hosting club.");

        if (eventStaffRepo.existsByEvent_EventIdAndMembership_MembershipId(eventId, membershipId))
            throw new ApiException(HttpStatus.CONFLICT, "The member has already been assigned to this event.");

        EventStaff staff = EventStaff.builder()
                .event(event)
                .membership(target)
                .duty(duty)
                .build();

        eventStaffRepo.save(staff);
        return mapToResponse(event);
    }


    @Override
    public List<EventStaffResponse> getEventStaffList(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));
        return eventStaffRepo.findByEvent_EventId(eventId).stream()
                .map(s -> EventStaffResponse.builder()
                        .id(s.getId())
                        .eventId(eventId)
                        .eventName(event.getName())
                        .membershipId(s.getMembership().getMembershipId())
                        .memberName(s.getMembership().getUser() != null
                                ? s.getMembership().getUser().getFullName() : null)
                        .duty(s.getDuty())
                        .state(s.getState())
                        .assignedAt(s.getAssignedAt())
                        .unassignedAt(s.getUnassignedAt())
                        .build())
                .toList();
    }

    @Override
    public Event getEntity(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));
    }


    @Override
    @Transactional
    public void delete(Long id) {
        if (!eventRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        }
        eventRepo.deleteById(id);
    }


    @Override
    public List<EventResponse> getAllEvents() {
        return eventRepo.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public List<EventRegistrationResponse> getRegisteredEventsByUser(Long userId) {
        return eventRegistrationRepo.findByUser_UserIdOrderByRegisteredAtDesc(userId)
                .stream()
                .map(r -> new EventRegistrationResponse(
                        r.getEvent().getEventId(),
                        r.getEvent().getName(),
                        r.getEvent().getDate(),
                        r.getStatus().name(),
                        (r.getEvent().getHostClub() != null)
                                ? r.getEvent().getHostClub().getName()
                                : "Unknown Club",
                        r.getCreatedAt()
                ))
                .toList();
    }


    @Override
    public List<EventResponse> getSettledEvents() {
        return eventRepo.findAllSettledEvents()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public EventResponse extendEvent(Long eventId, EventExtendRequest req) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.isCompleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot modify a completed event");
        }

        // 🔹 Parse chuỗi "HH:mm" linh hoạt (chấp nhận "8:00" hoặc "08:00")
        LocalTime newStart;
        LocalTime newEnd;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            newStart = LocalTime.parse(req.getNewStartTime().trim(), formatter);
            newEnd = LocalTime.parse(req.getNewEndTime().trim(), formatter);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid time format. Please use HH:mm (e.g. 09:00)");
        }

        if (newEnd.isBefore(newStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        if (req.getNewDate() != null) {
            event.setDate(req.getNewDate());
        }

        event.setStartTime(newStart);
        event.setEndTime(newEnd);
        eventRepo.save(event);

        // 🔹 Map thủ công thay vì dùng mapper (để tránh lỗi 'mapper not found')
        return EventResponse.builder()
                .id(event.getEventId())
                .name(event.getName())
                .description(event.getDescription())
                .date(event.getDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .status(event.getStatus())
                .checkInCode(event.getCheckInCode())
                .locationName(event.getLocation() != null ? event.getLocation().getName() : null)
                .commitPointCost(event.getCommitPointCost())
                .maxCheckInCount(event.getMaxCheckInCount())
                .currentCheckInCount(event.getCurrentCheckInCount())
                .hostClub(new EventResponse.SimpleClub(
                        event.getHostClub().getClubId(),
                        event.getHostClub().getName(),
                        EventCoHostStatusEnum.APPROVED
                ))
                .coHostedClubs(event.getCoHostRelations() == null ? List.of() :
                        event.getCoHostRelations().stream()
                                .map(rel -> new EventResponse.SimpleClub(
                                        rel.getClub().getClubId(),
                                        rel.getClub().getName(),
                                        rel.getStatus()))
                                .toList())
                .build();
    }

    @Transactional
    public EventResponse approveEventBudget(Long eventId, EventBudgetApproveRequest req, CustomUserDetails staff) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.APPROVED && event.getStatus() != EventStatusEnum.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved or completed events can receive budget grant");
        }

        Wallet eventWallet = event.getWallet();
        if (eventWallet == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event wallet not found");
        }

        // ✅ Ghi nhận ngân sách được duyệt
        long approvedPoints = req.getApprovedBudgetPoints().longValue();
        event.setBudgetPoints(approvedPoints);

        // ✅ Cập nhật ví
        eventWallet.setBalancePoints(eventWallet.getBalancePoints() + approvedPoints);

        // ✅ Giao dịch nạp điểm
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(eventWallet)
                .amount(approvedPoints)
                .type(WalletTransactionTypeEnum.EVENT_BUDGET_GRANT)
                .description("UniStaff granted " + approvedPoints + " points to event " + event.getName())
                .senderName(staff.getUser().getFullName())
                .receiverName(event.getName())
                .build();

        walletTransactionRepo.save(transaction);
        walletRepo.save(eventWallet);

        // ⚠️ BỔ SUNG DÒNG NÀY
        eventRepo.save(event); // Lưu lại giá trị budgetPoints mới

        return mapToResponse(event);
    }



    // =================================================================
// 🔹 HOÀN ĐIỂM SẢN PHẨM TRONG EVENT (EVENT_REFUND_PRODUCT)
// =================================================================
    @Transactional
    public WalletTransaction refundEventProduct(Long eventId, Long userId, Long productId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        Wallet eventWallet = event.getWallet();
        if (eventWallet == null)
            throw new ApiException(HttpStatus.NOT_FOUND, "Event wallet not found");

        walletRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        Long refundPoints = product.getPointCost();

        // ✅ Hoàn điểm lại cho ví sự kiện
        eventWallet.setBalancePoints(eventWallet.getBalancePoints() + refundPoints);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(eventWallet)
                .amount(refundPoints)
                .type(WalletTransactionTypeEnum.EVENT_REFUND_PRODUCT)
                .description("Refund product '" + product.getName() + "' for event " + event.getName())
                .build();

        walletTransactionRepo.save(transaction);
        walletRepo.save(eventWallet);

        log.info("♻️ [EVENT_REFUND_PRODUCT] Refunded {} points for product {} in event {}",
                refundPoints, product.getName(), event.getName());

        return transaction;
    }

    // =====================================================
// 🧩 Helper: Convert Event -> EventResponse
// =====================================================
    private EventResponse toEventResponse(Event event) {
        if (event == null) return null;

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
                .budgetPoints(event.getBudgetPoints())
                .locationName(event.getLocation() != null ? event.getLocation().getName() : null)
                .maxCheckInCount(event.getMaxCheckInCount())
                .currentCheckInCount(event.getCurrentCheckInCount())
                .commitPointCost(event.getCommitPointCost())
                .hostClub(event.getHostClub() != null
                        ? new EventResponse.SimpleClub(
                        event.getHostClub().getClubId(),
                        event.getHostClub().getName(),
                        null
                )
                        : null)
                .coHostedClubs(
                        event.getCoHostedClubs() != null
                                ? event.getCoHostedClubs().stream()
                                .map(club -> new EventResponse.SimpleClub(
                                        club.getClubId(),
                                        club.getName(),
                                        null
                                ))
                                .toList()
                                : null
                )
                .build();
    }
    @Override
    public byte[] exportAttendanceData(Long eventId, String format) {
        List<EventRegistration> list = eventRegistrationRepo.findByEvent_EventId(eventId);

        if (format.equalsIgnoreCase("csv")) {
            return CsvExportUtil.exportToCsv(list);
        } else {
            return ExcelExportUtil.exportToExcel(list);
        }
    }

}
