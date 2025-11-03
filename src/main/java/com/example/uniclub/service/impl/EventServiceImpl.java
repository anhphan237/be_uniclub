package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCreateRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final RewardService rewardService;
    private final WalletRepository walletRepo;
    private final EventStaffRepository eventStaffRepo;
    private final EventRegistrationRepository eventRegistrationRepo;

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
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ngày sự kiện không được ở quá khứ.");

        if (req.startTime() != null && req.endTime() != null && req.endTime().isBefore(req.startTime()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu.");

        Location location = locationRepo.findById(req.locationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Địa điểm không tồn tại."));

        if (req.maxCheckInCount() != null && req.maxCheckInCount() > location.getCapacity())
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Địa điểm chỉ chứa tối đa " + location.getCapacity() + " người.");

        Club hostClub = clubRepo.findById(req.hostClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLB tổ chức không tồn tại."));

        List<Club> coHosts = (req.coHostClubIds() != null && !req.coHostClubIds().isEmpty())
                ? clubRepo.findAllById(req.coHostClubIds()) : List.of();

        if (req.budgetPoints() == null || req.budgetPoints() <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ngân sách phải lớn hơn 0.");

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
                .rewardMultiplierCap(2)

                .build();

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
        // 🔹 1. Lấy event
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));

        // 🔹 DEBUG log đầu tiên
        log.info("DEBUG >>> userId={}, role=LEADER, state=ACTIVE", principal.getUser().getUserId());

        Long userId = principal.getUser().getUserId();

        // 🔹 2. Tìm membership của user có vai trò LEADER và đang ACTIVE
        Membership leaderMembership = membershipRepo
                .findByUser_UserIdAndClubRoleAndState(
                        userId,
                        ClubRoleEnum.LEADER,
                        MembershipStateEnum.ACTIVE
                )
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Bạn không phải là Leader hợp lệ của CLB nào."));

        // 🔹 DEBUG log: xác nhận membership
        log.info("DEBUG >>> LeaderMembership found: clubId={}", leaderMembership.getClub().getClubId());

        Club coClub = leaderMembership.getClub();

        // 🔹 DEBUG log: danh sách CLB đồng tổ chức trong event
        log.info("DEBUG >>> Event {} coHostRelations: {}", eventId,
                event.getCoHostRelations().stream().map(r -> r.getClub().getClubId()).toList());

        // 🔹 3. Kiểm tra CLB này có thật sự là Co-host của event hay không
        boolean isCoHost = event.getCoHostRelations().stream()
                .anyMatch(r -> Objects.equals(r.getClub().getClubId(), coClub.getClubId()));

        if (!isCoHost) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không phải là Co-host của sự kiện này.");
        }

        // 🔹 4. Lấy quan hệ EventCoClub cụ thể
        EventCoClub relation = event.getCoHostRelations().stream()
                .filter(r -> Objects.equals(r.getClub().getClubId(), coClub.getClubId()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Không tìm thấy quan hệ Co-host."));

        // 🔹 5. Cập nhật trạng thái phản hồi
        relation.setStatus(accepted ? EventCoHostStatusEnum.APPROVED : EventCoHostStatusEnum.REJECTED);
        relation.setRespondedAt(LocalDateTime.now());

        // 🔹 6. Log debug kết quả phản hồi
        log.info("CoHostRespond >>> eventId={}, coClub={}, accepted={}", eventId, coClub.getName(), accepted);

        // 🔹 7. Xử lý trạng thái sự kiện
        long approved = event.getCoHostRelations().stream()
                .filter(r -> r.getStatus() == EventCoHostStatusEnum.APPROVED).count();
        long rejected = event.getCoHostRelations().stream()
                .filter(r -> r.getStatus() == EventCoHostStatusEnum.REJECTED).count();
        long total = event.getCoHostRelations().size();

        if (total == 1 && rejected == 1) {
            event.setStatus(EventStatusEnum.REJECTED);
            eventRepo.save(event);
            notificationService.notifyHostEventRejectedByCoHost(event, coClub);
            return "❌ Co-host duy nhất '" + coClub.getName() + "' đã từ chối. Sự kiện bị hủy.";
        }

        if (approved > 0 && (approved + rejected == total)) {
            event.setStatus(EventStatusEnum.PENDING_UNISTAFF);

            // ✅ Giữ orphanRemoval, chỉ loại bỏ những cohost chưa approved thay vì replace list
            event.getCoHostRelations().removeIf(r -> r.getStatus() != EventCoHostStatusEnum.APPROVED);

            eventRepo.save(event);
            notificationService.notifyUniStaffReadyForReview(event);
            return "✅ Một số Co-host đã đồng ý. Sự kiện được gửi lên UniStaff duyệt.";
        }

        if (approved + rejected < total)
            return accepted
                    ? "✅ Co-host '" + coClub.getName() + "' đã đồng ý. Chờ các Co-host khác phản hồi."
                    : "❌ Co-host '" + coClub.getName() + "' đã từ chối. Chờ các Co-host khác phản hồi.";

        if (approved == total) {
            event.setStatus(EventStatusEnum.PENDING_UNISTAFF);
            eventRepo.save(event);
            notificationService.notifyUniStaffReadyForReview(event);
            return "✅ Tất cả Co-host đã đồng ý. Sự kiện chuyển sang chờ UniStaff duyệt.";
        }

        return "Phản hồi Co-host đã được ghi nhận.";
    }





    // =================================================================
    // 🔹 DUYỆT BỞI UNI STAFF
    // =================================================================
    @Override
    @Transactional
    public String reviewByUniStaff(Long eventId, boolean approve, CustomUserDetails principal, Integer budgetPoints) {
        Event event = eventRepo.findByIdWithCoHostRelations(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));

        String role = principal.getRoleName();
        if (!List.of("UNIVERSITY_STAFF", "ADMIN").contains(role))
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ UniStaff hoặc Admin có quyền duyệt sự kiện.");

        if (event.getStatus() != EventStatusEnum.PENDING_UNISTAFF)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Sự kiện chưa sẵn sàng để UniStaff duyệt.");

        // ❌ Nếu bị từ chối
        if (!approve) {
            event.setStatus(EventStatusEnum.REJECTED);
            eventRepo.saveAndFlush(event); // ✅ Flush ngay để cập nhật trạng thái
            notificationService.notifyEventRejected(event);
            return "❌ Sự kiện bị từ chối bởi UniStaff.";
        }

        // 🔹 Kiểm tra ngân sách hợp lệ
        if (budgetPoints == null || budgetPoints <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Vui lòng nhập ngân sách hợp lệ (>0).");

        // ✅ Cập nhật trạng thái và ngân sách
        event.setStatus(EventStatusEnum.APPROVED);


        // 🔹 Xử lý ví của sự kiện
        Wallet wallet = Optional.ofNullable(event.getWallet()).orElse(new Wallet());
        wallet.setOwnerType(WalletOwnerTypeEnum.EVENT);
        wallet.setEvent(event);

        // Nếu ví mới tạo → khởi tạo 0 điểm
        if (wallet.getBalancePoints() == null) wallet.setBalancePoints(0L);

        wallet.setBalancePoints(wallet.getBalancePoints() + budgetPoints);
        walletRepo.save(wallet);

        // 🔹 Gắn ví vào event và flush ngay để đảm bảo cập nhật DB
        event.setWallet(wallet);
        eventRepo.saveAndFlush(event);

        // 🔹 Gửi thông báo
        notificationService.notifyEventApproved(event);

        // 🔹 Log debug
        log.info("✅ [REVIEW_BY_UNISTAFF] Event {} approved with {} points by {}",
                event.getEventId(), budgetPoints, role);

        return "✅ Sự kiện '" + event.getName() + "' đã được UniStaff duyệt.";
    }


    // =================================================================
    // 🔹 KẾT THÚC SỰ KIỆN
    // =================================================================
    @Override
    @Transactional
    public String finishEvent(Long eventId, CustomUserDetails principal) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));

        var user = principal.getUser();
        boolean isUniStaff = user.getRole().getRoleName().equals("UNIVERSITY_STAFF");
        boolean isLeader = membershipRepo.existsByUser_UserIdAndClub_ClubIdAndClubRoleIn(
                user.getUserId(),
                event.getHostClub().getClubId(),
                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
        );

        if (!isUniStaff && !isLeader)
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền kết thúc sự kiện này.");

        if (!List.of(EventStatusEnum.APPROVED, EventStatusEnum.ONGOING).contains(event.getStatus()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ có thể kết thúc sự kiện đã duyệt hoặc đang diễn ra.");

        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);
        return "🟣 Sự kiện '" + event.getName() + "' đã được đánh dấu là hoàn thành.";
    }

    // =================================================================
    // 🔹 TRA CỨU & LỌC
    // =================================================================
    @Override
    public EventResponse get(Long id) {
        return eventRepo.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Mã check-in không hợp lệ."));
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));

        Membership actor = membershipRepo.findByUser_UserIdAndClub_ClubId(
                principal.getUser().getUserId(),
                event.getHostClub().getClubId()
        ).orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Bạn không phải thành viên CLB này."));

        if (!List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER).contains(actor.getClubRole()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ Leader hoặc Vice Leader có quyền gán Staff.");

        Membership target = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên."));
        if (!target.getClub().equals(event.getHostClub()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thành viên không thuộc CLB tổ chức.");

        if (eventStaffRepo.existsByEvent_EventIdAndMembership_MembershipId(eventId, membershipId))
            throw new ApiException(HttpStatus.CONFLICT, "Thành viên đã được gán vào sự kiện.");

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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện."));
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
    @Transactional
    public String submitEventToUniStaff(Long eventId, CustomUserDetails principal) {
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
            if (!allApproved)
                throw new ApiException(HttpStatus.BAD_REQUEST, "All co-host clubs must accept before submission.");
        }

        event.setStatus(EventStatusEnum.PENDING_UNISTAFF);
        eventRepo.save(event);
        notificationService.notifyUniStaffReadyForReview(event);

        return "📤 Event '" + event.getName() + "' submitted to UniStaff (PENDING_UNISTAFF).";
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
                                : "Unknown Club"
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

}
