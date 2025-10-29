package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCheckinRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventRegisterRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.AttendanceService;
import com.example.uniclub.service.EventPointsService;
import com.example.uniclub.service.JwtEventTokenService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventPointsServiceImpl implements EventPointsService {

    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final WalletService walletService;
    private final MembershipRepository membershipRepo;
    private final EventStaffRepository eventStaffRepo;
    private final JwtEventTokenService jwtEventTokenService;
    private final AttendanceService attendanceService;

    // =========================================================
    // 🔹 REGISTER
    // =========================================================
    @Override
    @Transactional
    public String register(CustomUserDetails principal, EventRegisterRequest req) {
        User user = userRepo.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.APPROVED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for registration");

        if (regRepo.existsByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId()))
            throw new ApiException(HttpStatus.CONFLICT, "You have already registered for this event");

        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "The event has already ended");

        Club hostClub = event.getHostClub();
        Membership membership = membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), hostClub.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You must be a member of the host club to join this event"));

        Wallet memberWallet = walletService.getOrCreateMembershipWallet(membership);
        Wallet eventWallet = ensureEventWallet(event);

        int commitPoints = event.getCommitPointCost();
        if (memberWallet.getBalancePoints() < commitPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points to register");

        // 💰 Chuyển điểm cam kết từ member → event wallet
        walletService.transferPoints(memberWallet, eventWallet, commitPoints,
                WalletTransactionTypeEnum.COMMIT_LOCK + ": Register event " + event.getName());

        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatusEnum.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .committedPoints(commitPoints)
                .attendanceLevel(AttendanceLevelEnum.NONE)
                .build();
        regRepo.save(reg);

        return "✅ Registered successfully. " + commitPoints + " points locked for commitment.";
    }

    // =========================================================
    // 🔹 CHECK-IN (50%)
    // =========================================================
    @Override
    @Transactional
    public String checkin(CustomUserDetails principal, EventCheckinRequest req) {
        String token = req.getEventJwtToken();
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing eventJwtToken.");
        }

        Long eventId = jwtEventTokenService.parseEventId(token);
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        // Xử lý check-in cho user hiện tại
        User user = principal.getUser();
        attendanceService.verifyAndSaveAttendance(user, event, req.getLevel());
        return "Check-in success for event: " + event.getName();
    }


    // =========================================================
    // 🔹 CANCEL REGISTRATION
    // =========================================================
    @Override
    @Transactional
    public String cancelRegistration(CustomUserDetails principal, Long eventId) {
        User user = principal.getUser();
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not registered for this event"));

        if (reg.getStatus() == RegistrationStatusEnum.CANCELED)
            return "ℹ️ Already canceled.";

        Wallet memberWallet = walletService.getOrCreateMembershipWallet(
                membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found")));
        Wallet eventWallet = ensureEventWallet(event);

        int refund = reg.getCommittedPoints();
        walletService.transferPoints(eventWallet, memberWallet, refund,
                WalletTransactionTypeEnum.REFUND_COMMIT + ": Cancel event registration");

        reg.setStatus(RegistrationStatusEnum.CANCELED);
        reg.setCanceledAt(LocalDateTime.now());
        regRepo.save(reg);

        return "❌ Registration canceled. " + refund + " points refunded.";
    }

    // =========================================================
// 🔹 END EVENT → REWARD + RETURN SURPLUS
// =========================================================
    @Override
    @Transactional
    public String endEvent(CustomUserDetails principal, EventEndRequest req) {
        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        Wallet eventWallet = ensureEventWallet(event);

        List<EventRegistration> regs = regRepo.findByEvent_EventId(event.getEventId());
        int totalReward = 0;

        for (EventRegistration reg : regs) {
            if (reg.getAttendanceLevel() == AttendanceLevelEnum.NONE) continue;

            int commit = reg.getCommittedPoints();
            int baseReward = switch (reg.getAttendanceLevel()) {
                case HALF -> commit;
                case FULL -> 2 * commit;
                default -> 0;
            };

            if (baseReward > 0) {
                Membership membership = membershipRepo
                        .findByUser_UserIdAndClub_ClubId(reg.getUser().getUserId(), event.getHostClub().getClubId())
                        .orElse(null);
                if (membership != null) {
                    double clubMultiplier = event.getHostClub().getClubMultiplier();
                    double memberMultiplier = membership.getMemberMultiplier();
                    double eventMultiplier = event.getType() == EventTypeEnum.SPECIAL ? 1.5 : 1.0;

                    int finalReward = (int) Math.round(baseReward * clubMultiplier * memberMultiplier * eventMultiplier);

                    walletService.transferPoints(eventWallet, membership.getWallet(), finalReward,
                            WalletTransactionTypeEnum.BONUS_REWARD.name() + " (multiplied)");

                    totalReward += finalReward;
                    reg.setStatus(RegistrationStatusEnum.REFUNDED);
                    regRepo.save(reg);
                }
            }
        }

        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);

        // 🔹 Return leftover
        int leftover = eventWallet.getBalancePoints().intValue();
        if (leftover > 0) {
            List<Club> clubs = new ArrayList<>(event.getCoHostedClubs());
            if (!clubs.contains(event.getHostClub())) clubs.add(event.getHostClub());
            int share = leftover / clubs.size();
            for (Club c : clubs) {
                Wallet clubWallet = walletService.getOrCreateClubWallet(c);
                walletService.transferPoints(eventWallet, clubWallet, share,
                        WalletTransactionTypeEnum.RETURN_SURPLUS + ": distribute remaining");
            }
        }

        eventWallet.setActive(false);
        walletRepo.save(eventWallet);

        return "🏁 Event completed. Total reward " + totalReward + " pts (multiplied); leftover returned.";
    }


    // =========================================================
    // 🔹 UTIL
    // =========================================================
    private Wallet ensureEventWallet(Event event) {
        Wallet w = event.getWallet();
        if (w == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event wallet not found. Please approve the event first.");
        return w;
    }

    // =========================================================
    // 🔹 SUPPORT GETTERS
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public List<EventRegistration> getEventRegistrations(Long eventId) {
        eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        return regRepo.findByEvent_EventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventSummary(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

        int totalCommit = regs.stream().mapToInt(r -> Optional.ofNullable(r.getCommittedPoints()).orElse(0)).sum();
        long refunded = regs.stream().filter(r -> r.getStatus() == RegistrationStatusEnum.REFUNDED).count();
        long checkedIn = regs.stream().filter(r -> r.getStatus() == RegistrationStatusEnum.CHECKED_IN).count();

        return Map.of(
                "eventName", event.getName(),
                "totalCommitPoints", totalCommit,
                "checkedInCount", checkedIn,
                "refundedCount", refunded,
                "registrationsCount", regs.size()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventWallet(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        Wallet wallet = ensureEventWallet(event);
        return Map.of(
                "eventId", event.getEventId(),
                "eventName", event.getName(),
                "walletBalance", wallet.getBalancePoints(),
                "ownerType", wallet.getOwnerType().name(),
                "hostClubId", event.getHostClub().getClubId(),
                "active", wallet.isActive()
        );
    }
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyRegisteredEvents(CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<EventRegistration> regs = regRepo.findByUser_UserIdOrderByRegisteredAtDesc(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (EventRegistration r : regs) {
            Event e = r.getEvent();
            result.add(Map.of(
                    "eventId", e.getEventId(),
                    "eventName", e.getName(),
                    "date", e.getDate(),
                    "status", e.getStatus().name(),
                    "attendanceLevel", r.getAttendanceLevel().name(),
                    "committedPoints", r.getCommittedPoints(),
                    "clubName", e.getHostClub().getName(),
                    "registeredAt", r.getRegisteredAt()
            ));
        }
        return result;
    }

}
