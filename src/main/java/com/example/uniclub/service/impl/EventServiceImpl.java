package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventBudgetApproveRequest;
import com.example.uniclub.dto.request.EventCreateRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventExtendRequest;
import com.example.uniclub.dto.response.EventRegistrationResponse;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
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
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepo;
    private final ClubRepository clubRepo;
    private final LocationRepository locationRepo;
    private final MembershipRepository membershipRepo;
    private final WalletRepository walletRepo;
    private final EventStaffRepository eventStaffRepo;
    private final EventRegistrationRepository eventRegistrationRepo;
    private final WalletTransactionRepository walletTransactionRepo;
    private final ProductRepository productRepo;
    private final EventRegistrationRepository regRepo;
    private final EventPointsService eventPointsService;
    private final EmailService emailService;


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

        if (req.date().isBefore(today))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event date cannot be in the past.");

        if (req.startTime() != null && req.endTime() != null && req.endTime().isBefore(req.startTime()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time.");

        if (req.date().isEqual(today) && req.startTime() != null && req.startTime().isBefore(LocalTime.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Start time must be after the current time.");

        Location location = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Location not found."));

        int maxCheckIn = (req.maxCheckInCount() != null && req.maxCheckInCount() > 0)
                ? req.maxCheckInCount()
                : location.getCapacity();

        if (maxCheckIn > location.getCapacity())
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This location can only accommodate up to " + location.getCapacity() + " people.");

        Club hostClub = clubRepo.findById(req.hostClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Host club not found."));

        List<Club> coHosts = (req.coHostClubIds() != null && !req.coHostClubIds().isEmpty())
                ? clubRepo.findAllById(req.coHostClubIds())
                : List.of();

        int commitCost = (req.type() == EventTypeEnum.PUBLIC) ? 0 :
                (req.commitPointCost() != null ? req.commitPointCost() : 0);

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
                .maxCheckInCount(maxCheckIn)
                .commitPointCost(commitCost)
                .budgetPoints(0L)
                .rewardMultiplierCap(2)
                .currentCheckInCount(0)
                .registrationDeadline(req.registrationDeadline())
                .build();

        if (req.type() == EventTypeEnum.PUBLIC) {
            event.setCommitPointCost(0);
            event.setRegistrationDeadline(null);
        } else if (req.type() == EventTypeEnum.SPECIAL || req.type() == EventTypeEnum.PRIVATE) {
            if (req.commitPointCost() == null || req.commitPointCost() <= 0)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Commit points required.");
            if (req.registrationDeadline() == null)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Registration deadline required.");
        }

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

        eventRepo.save(event);

        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.EVENT)
                .event(event)
                .balancePoints(0L)
                .status(WalletStatusEnum.ACTIVE)
                .build();

        walletRepo.save(wallet);
        event.setWallet(wallet);
        eventRepo.save(event);

        // ============================
        // 📧 EMAIL THÔNG BÁO
        // ============================
        if (coHosts.isEmpty()) {

            // Notify UniStaff: event waiting for review
            emailService.sendEventAwaitingUniStaffReviewEmail(
                    "unistaff@uniclub.id.vn", // hoặc lấy từ DB
                    event.getName(),
                    event.getDate().toString()
            );

        } else {
            // Send invite email to each co-host leader
            for (Club c : coHosts) {
                String leaderEmail = membershipRepo.findLeaderEmailByClubId(c.getClubId());
                if (leaderEmail != null) {
                    emailService.sendCoHostInviteEmail(
                            leaderEmail,
                            c.getName(),
                            event.getName()
                    );
                }
            }

            // Notify UniStaff: waiting for co-host responses
            emailService.sendEventWaitingUniStaffEmail(
                    "unistaff@uniclub.id.vn",
                    event.getName()
            );
        }

        return mapToResponse(event);
    }





    // =================================================================
    // 🔹 CO-HOST PHẢN HỒI
    // =================================================================
    @Override
    @Transactional
    public String respondCoHost(Long eventId, CustomUserDetails principal, boolean accepted) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        Long userId = principal.getUser().getUserId();

        Membership leaderMembership = membershipRepo
                .findByUser_UserIdAndClubRoleAndState(
                        userId,
                        ClubRoleEnum.LEADER,
                        MembershipStateEnum.ACTIVE
                )
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a valid leader."));

        Club coClub = leaderMembership.getClub();

        boolean isCoHost = event.getCoHostRelations().stream()
                .anyMatch(r -> Objects.equals(r.getClub().getClubId(), coClub.getClubId()));

        if (!isCoHost)
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a co-host of this event.");

        EventCoClub relation = event.getCoHostRelations().stream()
                .filter(r -> Objects.equals(r.getClub().getClubId(), coClub.getClubId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Relation not found."));

        relation.setStatus(accepted ? EventCoHostStatusEnum.APPROVED : EventCoHostStatusEnum.REJECTED);
        relation.setRespondedAt(LocalDateTime.now());


        long approved = event.getCoHostRelations().stream()
                .filter(r -> r.getStatus() == EventCoHostStatusEnum.APPROVED).count();
        long rejected = event.getCoHostRelations().stream()
                .filter(r -> r.getStatus() == EventCoHostStatusEnum.REJECTED).count();
        long total = event.getCoHostRelations().size();


        // 1️⃣ only one co-host & they rejected → cancel event
        if (total == 1 && rejected == 1) {
            event.setStatus(EventStatusEnum.REJECTED);
            eventRepo.save(event);

            String hostLeaderEmail =
                    membershipRepo.findLeaderEmailByClubId(event.getHostClub().getClubId());

            emailService.sendHostEventRejectedByCoHostEmail(
                    hostLeaderEmail,
                    event.getName(),
                    coClub.getName()
            );

            return "The only co-host rejected. Event cancelled.";
        }


        // 2️⃣ at least one approved, and all responded
        if (approved > 0 && (approved + rejected == total)) {
            event.setStatus(EventStatusEnum.PENDING_UNISTAFF);

            event.getCoHostRelations()
                    .removeIf(r -> r.getStatus() != EventCoHostStatusEnum.APPROVED);

            eventRepo.save(event);

            emailService.sendEventAwaitingUniStaffReviewEmail(
                    "unistaff@uniclub.id.vn",
                    event.getName(),
                    event.getDate().toString()
            );

            return "Event submitted to UniStaff review.";
        }


        // 3️⃣ waiting for other co-hosts
        if (approved + rejected < total) {
            return accepted
                    ? "Approved. Waiting for others."
                    : "Rejected. Waiting for others.";
        }


        // 4️⃣ all approved
        if (approved == total) {
            event.setStatus(EventStatusEnum.PENDING_UNISTAFF);
            eventRepo.save(event);

            emailService.sendEventAwaitingUniStaffReviewEmail(
                    "unistaff@uniclub.id.vn",
                    event.getName(),
                    event.getDate().toString()
            );

            return "All co-hosts approved. Event pending UniStaff.";
        }

        return "Response recorded.";
    }




    // =================================================================
// 🔹 KẾT THÚC SỰ KIỆN (CHUẨN)
// =================================================================
    @Override
    @Transactional
    public String finishEvent(Long eventId, CustomUserDetails principal) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        var user = principal.getUser();
        String roleName = user.getRole().getRoleName().toUpperCase();

        boolean isUniStaff = roleName.equals("UNIVERSITY_STAFF");
        boolean isLeader = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndClubRoleIn(
                user.getUserId(),
                event.getHostClub().getClubId(),
                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
        );

        if (!isUniStaff && !isLeader) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to finish this event.");
        }


        String result = eventPointsService.endEvent(principal, new EventEndRequest(eventId));

        log.info("🏁 Event '{}' completed by {} ({}) – Settlement executed",
                event.getName(), user.getEmail(), roleName);

        return result;
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

    @Override
    @Transactional
    public EventResponse approveEventBudget(Long eventId, EventBudgetApproveRequest req, CustomUserDetails staff) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        if (event.getStatus() == EventStatusEnum.REJECTED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "This event has already been rejected.");

        if (event.getStatus() != EventStatusEnum.PENDING_UNISTAFF
                && event.getStatus() != EventStatusEnum.APPROVED)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Only pending/approved events can be granted budget.");

        if (req.getApprovedBudgetPoints() == null || req.getApprovedBudgetPoints() <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Budget must be > 0.");

        long approvedPoints = req.getApprovedBudgetPoints();

        Wallet eventWallet = event.getWallet();
        if (eventWallet == null)
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Event wallet missing unexpectedly.");

        if (eventWallet.getStatus() == WalletStatusEnum.CLOSED)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet already closed.");

        eventWallet.setBalancePoints(approvedPoints);
        eventWallet.setStatus(WalletStatusEnum.ACTIVE);
        walletRepo.save(eventWallet);

        event.setApprovedBy(staff.getUser());
        event.setApprovedAt(LocalDateTime.now());
        event.setStatus(EventStatusEnum.APPROVED);
        event.setBudgetPoints(approvedPoints);
        eventRepo.save(event);

        WalletTransaction tx = WalletTransaction.builder()
                .wallet(eventWallet)
                .amount(approvedPoints)
                .type(WalletTransactionTypeEnum.EVENT_BUDGET_GRANT)
                .description("UniStaff approved " + approvedPoints + " points for event: " + event.getName())
                .senderName(staff.getUser().getFullName())
                .receiverName(event.getName())
                .createdAt(LocalDateTime.now())
                .build();

        walletTransactionRepo.save(tx);

        // EMAIL
        String leaderEmail = membershipRepo.findLeaderEmailByClubId(event.getHostClub().getClubId());

        emailService.sendEventApprovedEmail(
                leaderEmail,
                event.getName(),
                approvedPoints
        );

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
    @Transactional
    public String rejectEvent(Long eventId, String reason, CustomUserDetails staff) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.PENDING_UNISTAFF &&
                event.getStatus() != EventStatusEnum.PENDING_COCLUB) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event cannot be rejected at this stage");
        }

        event.setStatus(EventStatusEnum.REJECTED);
        event.setRejectReason(reason);
        event.setApprovedBy(staff.getUser());
        eventRepo.save(event);

        String hostLeaderEmail =
                membershipRepo.findLeaderEmailByClubId(event.getHostClub().getClubId());

        emailService.sendEventRejectedEmail(
                hostLeaderEmail,
                event.getName(),
                reason,
                staff.getUser().getFullName()
        );

        return "Event has been rejected successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventAttendanceSummary(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

        long totalRegistered = regs.size();
        long checkedInCount = regs.stream()
                .filter(r -> r.getAttendanceLevel() != null && r.getAttendanceLevel() != AttendanceLevelEnum.NONE)
                .count();

        long refundedCount = regs.stream()
                .filter(r -> r.getStatus() == RegistrationStatusEnum.REFUNDED)
                .count();

        long totalCommitPoints = regs.stream()
                .mapToLong(r -> Optional.ofNullable(r.getCommittedPoints()).orElse(0))
                .sum();

        return Map.of(
                "eventId", event.getEventId(),
                "eventName", event.getName(),
                "status", event.getStatus().name(),
                "totalRegistered", totalRegistered,
                "checkedInCount", checkedInCount,
                "refundedCount", refundedCount,
                "totalCommitPoints", totalCommitPoints
        );
    }




}
