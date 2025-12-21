package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.*;
import com.example.uniclub.dto.response.EventDayResponse;
import com.example.uniclub.dto.response.EventRegistrationResponse;
import com.example.uniclub.dto.response.EventResponse;
import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.EventPointsService;
import com.example.uniclub.service.EventService;
import com.example.uniclub.util.CsvExportUtil;
import com.example.uniclub.util.ExcelExportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


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
    private final EventDayRepository eventDayRepo;
    private final AttendanceRecordRepository attendanceRepo;


    // =================================================================
    // 🔹 MAPPER
    // =================================================================
    private EventResponse mapToResponse(Event event) {

        // ===== MAP EVENT DAYS (AN TOÀN GENERICS) =====
        List<EventDayResponse> dayResponses = Collections.emptyList();
        if (event.getDays() != null) {
            dayResponses = event.getDays().stream()
                    .map(d -> new EventDayResponse(
                            d.getId(),
                            d.getDate(),
                            d.getStartTime().toString(),
                            d.getEndTime().toString()
                    ))
                    .collect(Collectors.toList());
        }


        // ===== MAP CO-HOST CLUBS (AN TOÀN GENERICS) =====
        List<EventResponse.SimpleClub> coHostedClubs = Collections.emptyList();
        if (event.getCoHostRelations() != null) {
            coHostedClubs = event.getCoHostRelations().stream()
                    .map(rel -> new EventResponse.SimpleClub(
                            rel.getClub().getClubId(),
                            rel.getClub().getName(),
                            rel.getStatus()
                    ))
                    .collect(Collectors.toList());
        }

        return EventResponse.builder()
                .id(event.getEventId())
                .name(event.getName())
                .description(event.getDescription())
                .type(event.getType())

                .startDate(event.getStartDate())
                .endDate(event.getEndDate())

                .days(dayResponses)

                .status(event.getStatus())
                .checkInCode(event.getCheckInCode())
                .commitPointCost(event.getCommitPointCost())
                .budgetPoints(event.getBudgetPoints())
                .locationName(
                        event.getLocation() != null
                                ? event.getLocation().getName()
                                : null
                )

                .maxCheckInCount(event.getMaxCheckInCount())
                .currentCheckInCount(event.getCurrentCheckInCount())

                .hostClub(new EventResponse.SimpleClub(
                        event.getHostClub().getClubId(),
                        event.getHostClub().getName(),
                        EventCoHostStatusEnum.APPROVED
                ))

                .coHostedClubs(coHostedClubs)

                .build();
    }


    // =================================================================
    // 🔹 TẠO SỰ KIỆN
    // =================================================================
    @Override
    @Transactional
    public EventResponse create(EventCreateRequest req) {

        LocalDate today = LocalDate.now();

        // -------------------------------------------------------------
        // 🔍 Validate danh sách ngày
        // -------------------------------------------------------------
        if (req.days() == null || req.days().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event must have at least 1 day.");
        }

        for (EventDayRequest day : req.days()) {

            if (day.getDate().isBefore(today)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Event day " + day.getDate() + " cannot be in the past.");
            }

            if (day.getStartTime().isAfter(day.getEndTime())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Start time must be before end time for date " + day.getDate());
            }

            if (day.getDate().isEqual(today)
                    && day.getStartTime().isBefore(LocalTime.now())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Start time for today must be in the future.");
            }
        }

        // -------------------------------------------------------------
        // 🔥 CHECK LOCATION–TIME CONFLICT
        // -------------------------------------------------------------
        for (EventDayRequest d : req.days()) {
            List<Event> conflicts = eventRepo.findConflictedEvents(
                    req.locationId(),
                    d.getDate(),
                    d.getStartTime(),
                    d.getEndTime()
            );

            if (!conflicts.isEmpty()) {
                Event c = conflicts.get(0);
                throw new ApiException(HttpStatus.CONFLICT,
                        "Location '" + c.getLocation().getName() + "' is already booked by event '"
                                + c.getName() + "' on " + d.getDate() + " from "
                                + d.getStartTime() + " to " + d.getEndTime());
            }
        }

        // -------------------------------------------------------------
        // 📅 Tính ngày bắt đầu / kết thúc
        // -------------------------------------------------------------
        LocalDate startDate = req.days().stream()
                .map(EventDayRequest::getDate)
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate endDate = req.days().stream()
                .map(EventDayRequest::getDate)
                .max(LocalDate::compareTo)
                .orElseThrow();

        // -------------------------------------------------------------
        // 📍 Validate location
        // -------------------------------------------------------------
        Location location = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Location not found."));

        int maxCheckIn = (req.maxCheckInCount() != null && req.maxCheckInCount() > 0)
                ? req.maxCheckInCount()
                : location.getCapacity();

        if (maxCheckIn > location.getCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This location can only accommodate up to " + location.getCapacity() + " people.");
        }

        // -------------------------------------------------------------
        // 🏛 CLB chủ trì & đồng chủ trì
        // -------------------------------------------------------------
        Club hostClub = clubRepo.findById(req.hostClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Host club not found."));

        List<Club> coHosts = (req.coHostClubIds() != null && !req.coHostClubIds().isEmpty())
                ? clubRepo.findAllById(req.coHostClubIds())
                : List.of();

        // -------------------------------------------------------------
        // 🔒 Validate commit point cost (base rule)
        // -------------------------------------------------------------
        if (req.commitPointCost() != null && req.commitPointCost() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Commit point cost must be >= 0");
        }

        int commitCost = req.commitPointCost() != null
                ? req.commitPointCost()
                : 0;

        // -------------------------------------------------------------
        // 🧱 Tạo EVENT (base)
        // -------------------------------------------------------------
        Event event = Event.builder()
                .hostClub(hostClub)
                .name(req.name())
                .description(req.description())
                .type(req.type())
                .startDate(startDate)
                .endDate(endDate)
                .location(location)
                .status(coHosts.isEmpty()
                        ? EventStatusEnum.PENDING_UNISTAFF
                        : EventStatusEnum.PENDING_COCLUB)
                .checkInCode("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .maxCheckInCount(maxCheckIn)
                .commitPointCost(commitCost)
                .budgetPoints(0L)
                .rewardMultiplierCap(2)
                .currentCheckInCount(0)
                .registrationDeadline(req.registrationDeadline())
                .build();

        // -------------------------------------------------------------
        // 🎯 Validate & SET theo loại sự kiện
        // -------------------------------------------------------------
        if (req.type() == EventTypeEnum.PUBLIC) {

            // ❌ PUBLIC bắt buộc có rewardPerParticipant
            if (req.rewardPerParticipant() == null || req.rewardPerParticipant() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Public event requires rewardPerParticipant > 0");
            }

            // ❌ PUBLIC không được có commitPointCost
            if (req.commitPointCost() != null && req.commitPointCost() > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "commitPointCost is not allowed for PUBLIC event");
            }

            event.setRewardPerParticipant(req.rewardPerParticipant());
            event.setCommitPointCost(0);
            event.setRegistrationDeadline(null);

        } else if (req.type() == EventTypeEnum.SPECIAL || req.type() == EventTypeEnum.PRIVATE) {

            // ❌ PRIVATE / SPECIAL bắt buộc commit
            if (req.commitPointCost() == null || req.commitPointCost() <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Commit points required.");
            }

            // ❌ PRIVATE / SPECIAL không được có rewardPerParticipant
            if (req.rewardPerParticipant() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "rewardPerParticipant is only allowed for PUBLIC event");
            }

            if (req.registrationDeadline() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Registration deadline required.");
            }

            if (req.registrationDeadline().isAfter(startDate)) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Registration deadline cannot be after the first event day.");
            }

            event.setCommitPointCost(req.commitPointCost());
            event.setRewardPerParticipant(null);
            event.setRegistrationDeadline(req.registrationDeadline());
        }

        // -------------------------------------------------------------
        // 🔗 Đồng tổ chức (Co-host relations)
        // -------------------------------------------------------------
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

        // -------------------------------------------------------------
        // 💾 Lưu EVENT
        // -------------------------------------------------------------
        eventRepo.save(event);
        eventRepo.flush();

        // -------------------------------------------------------------
        // 📆 Lưu EVENT DAYS
        // -------------------------------------------------------------
        List<EventDay> eventDays = req.days().stream()
                .map(d -> EventDay.builder()
                        .event(event)
                        .date(d.getDate())
                        .startTime(d.getStartTime())
                        .endTime(d.getEndTime())
                        .build())
                .toList();

        eventDayRepo.saveAll(eventDays);
        event.setDays(eventDays);

        // -------------------------------------------------------------
        // 💳 Tạo ví EVENT
        // -------------------------------------------------------------
        Wallet wallet = Wallet.builder()
                .ownerType(WalletOwnerTypeEnum.EVENT)
                .event(event)
                .balancePoints(0L)
                .status(WalletStatusEnum.ACTIVE)
                .build();

        walletRepo.save(wallet);
        event.setWallet(wallet);

        // -------------------------------------------------------------
        // 📧 EMAIL
        // -------------------------------------------------------------
        if (coHosts.isEmpty()) {
            emailService.sendEventAwaitingUniStaffReviewEmail(
                    "unistaff@uniclub.id.vn",
                    event.getName(),
                    startDate.toString()
            );
        } else {
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

            emailService.sendEventWaitingUniStaffEmail(
                    "unistaff@uniclub.id.vn",
                    event.getName()
            );
        }

        return mapToResponse(event);
    }


    // =================================================================
    // 🔹 CO-HOST PHẢN HỒI (MULTI-DAY FIXED)
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

            // remove non-approved relations
            event.getCoHostRelations()
                    .removeIf(r -> r.getStatus() != EventCoHostStatusEnum.APPROVED);

            eventRepo.save(event);

            emailService.sendEventAwaitingUniStaffReviewEmail(
                    "unistaff@uniclub.id.vn",
                    event.getName(),
                    event.getStartDate().toString()   // 🔥 FIXED
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
                    event.getStartDate().toString()   // 🔥 FIXED
            );

            return "All co-hosts approved. Event pending UniStaff.";
        }

        return "Response recorded.";
    }

    // =================================================================
    // 🔹 KẾT THÚC SỰ KIỆN (MULTI-DAY SUPPORTED)
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
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only Leader or University Staff may finish this event.");
        }

        // 1️⃣ Must be APPROVED or ONGOING
        if (!List.of(EventStatusEnum.APPROVED, EventStatusEnum.ONGOING)
                .contains(event.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event must be APPROVED or ONGOING to finish.");
        }

        // 2️⃣ Not finished already
        if (event.getCompletedAt() != null || event.getStatus() == EventStatusEnum.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event has already been settled.");
        }

        // 3️⃣ Validate wallet
        if (event.getWallet() == null)
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Event wallet missing unexpectedly.");

        if (event.getWallet().getStatus() == WalletStatusEnum.CLOSED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event wallet is closed. Cannot finish event.");

        // ❗❗ MULTI-DAY LOGIC
        // Leader/UniStaff có thể kết thúc sớm → không validate thời gian.
        // Nhưng nếu muốn check event đã tự kết thúc thì dùng:
        //
        // if (LocalDate.now().isBefore(event.getEndDate())) {
        //     throw new ApiException(BAD_REQUEST,
        //             "Event has not ended yet. Last day is " + event.getEndDate());
        // }
        //
        // Tuy nhiên do bạn CHỦ ĐÍCH muốn cho phép kết thúc sớm → bỏ check này.

        // 4️⃣ Run settlement
        String result = eventPointsService.endEvent(principal, new EventEndRequest(eventId));

        event.setStatus(EventStatusEnum.COMPLETED);
        event.setCompletedAt(LocalDateTime.now());
        eventRepo.save(event);

        log.info("🏁 Event '{}' ({} → {}) completed EARLY/NORMAL by {} ({}) – Settlement executed",
                event.getName(),
                event.getStartDate(),   // 🔥 multi-day
                event.getEndDate(),     // 🔥 multi-day
                user.getEmail(),
                roleName
        );
// 🔥 EXPIRE ALL STAFF WHEN EVENT COMPLETED
        List<EventStaff> staffs = eventStaffRepo.findByEvent_EventId(eventId);
        for (EventStaff s : staffs) {
            if (s.getState() == EventStaffStateEnum.ACTIVE) {
                s.setState(EventStaffStateEnum.EXPIRED);
                s.setUnassignedAt(LocalDateTime.now());
            }
        }
        eventStaffRepo.saveAll(staffs);

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
        String keyword = (name == null) ? "" : name;

        Page<Event> page = eventRepo.filterEvents(keyword, date, status, pageable);

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
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByLocation(Long locationId) {

        List<Event> events = eventRepo.findEventsByLocationWithDays(locationId);

        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public List<EventResponse> getUpcomingEvents() {
        LocalDate today = LocalDate.now();

        return eventRepo.findUpcomingEvents(today).stream()
                .filter(e -> e.getStatus() == EventStatusEnum.APPROVED) // 🔥 upcoming = APPROVED
                .sorted(Comparator.comparing(Event::getStartDate))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<EventResponse> getMyEvents(CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        return eventRepo.findEventsByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<EventResponse> getActiveEvents() {
        LocalDate today = LocalDate.now();

        return eventRepo.findActiveEvents(today).stream()
                .map(this::mapToResponse)
                .toList();
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

        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        // 1️⃣ Không được xóa event đã completed / cancelled
        if (event.getStatus() == EventStatusEnum.COMPLETED ||
                event.getStatus() == EventStatusEnum.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Completed or cancelled events cannot be deleted. Use cancel instead.");
        }

        // 2️⃣ Không xóa event đã có người đăng ký
        if (eventRegistrationRepo.existsByEvent_EventId(event.getEventId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete event with registrations. Cancel the event instead.");
        }

        // 3️⃣ Không xóa event đã có staff
        if (!eventStaffRepo.findByEvent_EventId(event.getEventId()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete event with assigned staff.");
        }

        // 4️⃣ Không xóa nếu có ngân sách
        if (event.getBudgetPoints() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete event after budget approval. Cancel instead.");
        }

        // 5️⃣ Không xóa event đã có attendance
        if (eventRegistrationRepo.existsByEvent_EventIdAndAttendanceLevelNot(
                id, AttendanceLevelEnum.NONE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event contains attendance records and cannot be deleted.");
        }

        // 6️⃣ Không xóa nếu còn co-host
        if (event.getCoHostRelations() != null && !event.getCoHostRelations().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot delete event with co-host relations. Must cancel instead.");
        }

        // ===========================
        // 7️⃣ Safe delete
        // ===========================
        if (event.getWallet() != null) {
            walletRepo.delete(event.getWallet()); // để tránh orphan
        }

        eventRepo.delete(event);
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
                .map(r -> {
                    var event = r.getEvent();
                    return new EventRegistrationResponse(
                            event.getEventId(),
                            event.getName(),
                            event.getStartDate(),
                            event.getEndDate(),
                            r.getStatus().name(),
                            (event.getHostClub() != null)
                                    ? event.getHostClub().getName()
                                    : "Unknown Club",
                            r.getCreatedAt()
                    );
                })
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
    @Transactional
    public EventResponse extendEvent(Long eventId, EventExtendRequest req) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.isCompleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot modify a completed event");
        }

        if (req.getDayId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dayId is required");
        }

        EventDay day = eventDayRepo.findById(req.getDayId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event day not found"));

        if (!day.getEvent().getEventId().equals(eventId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This day does not belong to the event");
        }

        // ===================== 1️⃣ Parse time ======================
        LocalTime newStart, newEnd;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            newStart = LocalTime.parse(req.getNewStartTime().trim(), formatter);
            newEnd   = LocalTime.parse(req.getNewEndTime().trim(), formatter);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid time format. Use HH:mm (e.g. 09:00)");
        }

        if (newEnd.isBefore(newStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
// 🔥 CHECK LOCATION–TIME CONFLICT
        List<Event> conflicts = eventRepo.findConflictedEvents(
                        event.getLocation().getLocationId(),
                        day.getDate(),
                        newStart,
                        newEnd
                ).stream()
                .filter(e -> !e.getEventId().equals(eventId)) // ignore current event
                .toList();

        if (!conflicts.isEmpty()) {
            Event c = conflicts.get(0);
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot update schedule. Location is already booked by event: " + c.getName());
        }

        // ===================== 3️⃣ Apply update ======================
        day.setStartTime(newStart);
        day.setEndTime(newEnd);

        eventDayRepo.save(day);

        // ===================== 4️⃣ Recompute start/end ======================
        eventRepo.save(event);

        // ===================== 5️⃣ Return ======================
        return mapToResponse(event);
    }


    @Override
    @Transactional
    public EventResponse approveEventBudget(
            Long eventId,
            EventBudgetApproveRequest req,
            CustomUserDetails staff
    ) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        // ❌ Event đã bị reject
        if (event.getStatus() == EventStatusEnum.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This event has already been rejected.");
        }

        // ❌ Sai trạng thái
        if (event.getStatus() != EventStatusEnum.PENDING_UNISTAFF
                && event.getStatus() != EventStatusEnum.APPROVED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Only pending/approved events can be granted budget."
            );
        }

        // ❌ Budget không hợp lệ
        if (req.getApprovedBudgetPoints() == null || req.getApprovedBudgetPoints() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Budget must be > 0.");
        }

        long approvedPoints = req.getApprovedBudgetPoints();

        // =========================================================
        // 🔥 VALIDATE THEO LOẠI EVENT
        // =========================================================

        if (event.getType() == EventTypeEnum.PUBLIC) {

            // PUBLIC bắt buộc có rewardPerParticipant
            if (event.getRewardPerParticipant() == null || event.getRewardPerParticipant() <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Public event must define rewardPerParticipant > 0"
                );
            }

            // PUBLIC bắt buộc có maxCheckInCount
            Integer max = event.getMaxCheckInCount();
            if (max == null || max <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Public event must define maxCheckInCount"
                );
            }

            long requiredBudget =
                    (long) event.getRewardPerParticipant() * max;

            if (approvedPoints < requiredBudget) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Approved budget is insufficient for PUBLIC event. "
                                + "Required: " + requiredBudget
                );
            }

        } else {
            // PRIVATE / SPECIAL không được có rewardPerParticipant
            if (event.getRewardPerParticipant() != null && event.getRewardPerParticipant() > 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "rewardPerParticipant is only applicable for PUBLIC events"
                );
            }
        }

        // =========================================================
        // 💳 EVENT WALLET
        // =========================================================
        Wallet eventWallet = event.getWallet();
        if (eventWallet == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Event wallet missing unexpectedly."
            );
        }

        if (eventWallet.getStatus() == WalletStatusEnum.CLOSED) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Event wallet already closed."
            );
        }

        // 👉 APPROVE 1 LẦN → overwrite balance
        eventWallet.setBalancePoints(approvedPoints);
        eventWallet.setStatus(WalletStatusEnum.ACTIVE);
        walletRepo.save(eventWallet);

        // =========================================================
        // 🏦 UNIVERSITY WALLET (SOURCE)
        // =========================================================
        Wallet uniWallet = walletRepo.findUniversityWallet()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "University wallet not found."
                ));

        // =========================================================
        // 🧾 TRANSACTION LOG
        // =========================================================
        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .wallet(uniWallet)
                        .amount(approvedPoints)
                        .type(WalletTransactionTypeEnum.EVENT_BUDGET_GRANT)
                        .description(
                                "UniStaff approved " + approvedPoints
                                        + " points for event: " + event.getName()
                        )
                        .senderName(staff.getUser().getFullName())
                        .receiverName(event.getName())
                        .receiverClub(event.getHostClub())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // =========================================================
        // UPDATE EVENT
        // =========================================================
        event.setApprovedBy(staff.getUser());
        event.setApprovedAt(LocalDateTime.now());
        event.setStatus(EventStatusEnum.APPROVED);
        event.setBudgetPoints(approvedPoints);
        eventRepo.save(event);

        // =========================================================
        // EMAIL
        // =========================================================
        String leaderEmail =
                membershipRepo.findLeaderEmailByClubId(event.getHostClub().getClubId());

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

        long totalRegistered = 0;
        long checkedInCount = 0;
        long refundedCount = 0;
        long totalCommitPoints = 0;

        if (event.getType() == EventTypeEnum.PUBLIC) {

            // ✅ PUBLIC: chỉ đếm attendance_records
            checkedInCount = attendanceRepo.countPublicCheckedIn(eventId);

        } else {

            List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

            totalRegistered = regs.size();

            checkedInCount = regs.stream()
                    .filter(r -> r.getAttendanceLevel() != null
                            && r.getAttendanceLevel() != AttendanceLevelEnum.NONE)
                    .count();

            refundedCount = regs.stream()
                    .filter(r -> r.getStatus() == RegistrationStatusEnum.REWARDED)
                    .count();

            totalCommitPoints = regs.stream()
                    .mapToLong(r -> Optional.ofNullable(r.getCommittedPoints()).orElse(0))
                    .sum();
        }

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



    @Override
    @Transactional
    public String cancelEvent(Long eventId, EventCancelRequest req, CustomUserDetails principal) {

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        var user = principal.getUser();

        // 1) CHECK QUYỀN
        boolean isUniStaff = user.getRole().getRoleName().equalsIgnoreCase("UNIVERSITY_STAFF");

        boolean isLeader = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndClubRoleIn(
                user.getUserId(),
                event.getHostClub().getClubId(),
                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
        );

        if (!isUniStaff && !isLeader) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only Leader or University Staff can cancel the event");
        }

        // 2) CHỈ HỦY SỰ KIỆN CHƯA DIỄN RA
        // 🔥 Kiểm tra multi-day
        EventDay earliest = event.getDays().stream()
                .sorted(Comparator.comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Event has no days"));

        LocalDateTime earliestStart = LocalDateTime.of(earliest.getDate(), earliest.getStartTime());

        if (LocalDateTime.now().isAfter(earliestStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel an event that has already started");
        }

        // 3) CHECK TRẠNG THÁI HỢP LỆ
        if (!(event.getStatus() == EventStatusEnum.APPROVED
                || event.getStatus() == EventStatusEnum.PENDING_UNISTAFF
                || event.getStatus() == EventStatusEnum.PENDING_COCLUB)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event cannot be cancelled in its current state");
        }

        // 4) CHECK CHƯA ĐIỂM DANH
        boolean hasAttendance = eventRegistrationRepo.existsByEvent_EventIdAndAttendanceLevelNot(
                eventId, AttendanceLevelEnum.NONE);
        if (hasAttendance) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel event with attendance records");
        }

        // 5) HOÀN COMMIT POINT + CANCEL REGISTRATION
        List<EventRegistration> regs = eventRegistrationRepo.findByEvent_EventId(eventId);
        for (EventRegistration reg : regs) {
            int committed = Optional.ofNullable(reg.getCommittedPoints()).orElse(0);
            if (committed > 0) {
                eventPointsService.refundCommitPoints(reg.getUser(), committed, event);
            }
            reg.setStatus(RegistrationStatusEnum.CANCELED);
            reg.setCancelledAt(LocalDateTime.now());
        }
        eventRegistrationRepo.saveAll(regs);

        // 6) HỦY STAFF ASSIGNMENT
        List<EventStaff> staffs = eventStaffRepo.findByEvent_EventId(eventId);
        for (EventStaff s : staffs) {
            s.setState(EventStaffStateEnum.REMOVED);
            s.setUnassignedAt(LocalDateTime.now());
        }
        eventStaffRepo.saveAll(staffs);

        // 7) XỬ LÝ BUDGET TUỲ THEO AI HỦY
        Wallet eventWallet = event.getWallet();

        if (eventWallet != null && eventWallet.getBalancePoints() > 0) {

            if (isUniStaff) {

                Wallet clubWallet = walletRepo.findByClub_ClubId(event.getHostClub().getClubId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                "Club wallet not found"));

                long refund = eventWallet.getBalancePoints();
                clubWallet.setBalancePoints(clubWallet.getBalancePoints() + refund);
                eventWallet.setBalancePoints(0L);

                walletRepo.save(clubWallet);
                walletRepo.save(eventWallet);

                walletTransactionRepo.save(WalletTransaction.builder()
                        .wallet(clubWallet)
                        .amount(refund)
                        .type(WalletTransactionTypeEnum.EVENT_REFUND_REMAINING)
                        .description("Refund remaining event budget (cancelled by UniStaff): " + event.getName())
                        .createdAt(LocalDateTime.now())
                        .build()
                );

            } else {
                walletTransactionRepo.save(WalletTransaction.builder()
                        .wallet(eventWallet)
                        .amount(0L)
                        .type(WalletTransactionTypeEnum.EVENT_BUDGET_FORFEIT)
                        .description("Budget forfeited because event was cancelled by club leader")
                        .createdAt(LocalDateTime.now())
                        .build()
                );

                eventWallet.setStatus(WalletStatusEnum.CLOSED);
                walletRepo.save(eventWallet);
            }
        }

        // 8) SET STATUS = CANCELLED
        event.setRejectReason(req.reason());
        event.setStatus(EventStatusEnum.CANCELLED);
        event.setCancelledAt(LocalDateTime.now());
        eventRepo.save(event);

        // 9) EMAIL (multi-day: gửi theo startDate - endDate)
        String leaderEmail = membershipRepo.findLeaderEmailByClubId(event.getHostClub().getClubId());
        try {
            emailService.sendEventCancelledEmail(
                    leaderEmail,
                    event.getName(),
                    event.getStartDate() + " - " + event.getEndDate(),
                    req.reason()
            );
        } catch (Exception ignored) {}

        return "Event has been cancelled successfully";
    }
    @Override
    public List<EventResponse> getEventsByDate(LocalDate date) {
        List<Event> events = eventRepo.findEventsByDate(date);
        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }

}
