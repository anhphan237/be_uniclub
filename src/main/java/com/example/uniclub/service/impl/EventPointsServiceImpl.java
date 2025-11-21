package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCheckinRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventRegisterRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPointsServiceImpl implements EventPointsService {

    private final EventLogService eventLogService;
    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final WalletService walletService;
    private final MembershipRepository membershipRepo;
    private final JwtEventTokenService jwtEventTokenService;
    private final AttendanceService attendanceService;
    private final EmailService emailService;
    private final RewardService rewardService;

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

        if (event.getType() == EventTypeEnum.PUBLIC)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Public events do not require registration.");

        if (!List.of(EventStatusEnum.APPROVED, EventStatusEnum.ONGOING).contains(event.getStatus()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for registration.");

        if (regRepo.existsByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId()))
            throw new ApiException(HttpStatus.CONFLICT, "You have already registered for this event.");

        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "The event has already ended.");

        // Check membership rules
        if (event.getType() == EventTypeEnum.PRIVATE) {
            boolean isHostMember = membershipRepo.existsByUser_UserIdAndClub_ClubId(
                    user.getUserId(), event.getHostClub().getClubId());
            if (!isHostMember)
                throw new ApiException(HttpStatus.FORBIDDEN, "Private event: only host club members can register.");
        }
        else if (event.getType() == EventTypeEnum.SPECIAL) {
            boolean isMember = membershipRepo.existsByUser_UserIdAndClub_ClubId(
                    user.getUserId(), event.getHostClub().getClubId()
            ) || event.getCoHostedClubs().stream().anyMatch(
                    c -> membershipRepo.existsByUser_UserIdAndClub_ClubId(user.getUserId(), c.getClubId())
            );
            if (!isMember)
                throw new ApiException(HttpStatus.FORBIDDEN, "You must be a member of host or cohost club to join this event.");
        }

        // Commit points
        Wallet memberWallet = walletService.getOrCreateUserWallet(user);
        Wallet eventWallet = ensureEventWallet(event);

        long commitPoints = Optional.ofNullable(event.getCommitPointCost()).orElse(0);
        if (memberWallet.getBalancePoints() < commitPoints)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points to register.");

        walletService.transferPointsWithType(
                memberWallet, eventWallet, commitPoints,
                "Register event " + event.getName(),
                WalletTransactionTypeEnum.COMMIT_LOCK
        );

        regRepo.save(EventRegistration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatusEnum.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .committedPoints((int) commitPoints)
                .attendanceLevel(AttendanceLevelEnum.NONE)
                .build());

        // 📩 SEND EMAIL
        emailService.sendEventRegistrationEmail(
                user.getEmail(),
                user.getFullName(),
                event,
                commitPoints
        );

        return "Registered successfully. " + commitPoints + " points locked for commitment.";
    }




    // =========================================================
    // 🔹 CHECK-IN
    // =========================================================
    @Override
    @Transactional
    public String checkin(CustomUserDetails principal, EventCheckinRequest req) {
        String token = req.getEventJwtToken();
        if (token == null || token.isBlank())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing eventJwtToken.");

        Long eventId = jwtEventTokenService.parseEventId(token);
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));
        User user = principal.getUser();

        // ⚠️ PUBLIC: chỉ check-in 1 lần
        if (event.getType() == EventTypeEnum.PUBLIC) {
            attendanceService.handlePublicCheckin(user, event);
            eventLogService.logAction(user.getUserId(), user.getFullName(),
                    event.getEventId(), event.getName(),
                    UserActionEnum.CHECKIN_EVENT,
                    "User performed PUBLIC check-in for event " + event.getName());
            return "✅ Checked in successfully for PUBLIC event: " + event.getName();
        }

        // SPECIAL / PRIVATE: 3 phase
        switch (req.getLevel().toUpperCase()) {
            case "START" -> attendanceService.handleStartCheckin(user, event);
            case "MID" -> attendanceService.handleMidCheckin(user, event);
            case "END" -> attendanceService.handleEndCheckout(user, event);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid check-in phase: " + req.getLevel());
        }

        eventLogService.logAction(user.getUserId(), user.getFullName(),
                event.getEventId(), event.getName(),
                req.getLevel().equalsIgnoreCase("END") ? UserActionEnum.CHECKOUT_EVENT : UserActionEnum.CHECKIN_EVENT,
                "User performed " + req.getLevel().toUpperCase() + " check-in for event " + event.getName());

        return "✅ " + req.getLevel() + " check-in successful for event: " + event.getName();
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
            return "Already canceled.";

        Wallet memberWallet = walletService.getOrCreateUserWallet(user);
        Wallet eventWallet = ensureEventWallet(event);

        long refund = Optional.ofNullable(reg.getCommittedPoints()).orElse(0);
        walletService.transferPointsWithType(
                eventWallet, memberWallet, refund,
                "Cancel event registration " + event.getName(),
                WalletTransactionTypeEnum.REFUND_COMMIT
        );

        reg.setStatus(RegistrationStatusEnum.CANCELED);
        reg.setCanceledAt(LocalDateTime.now());
        regRepo.save(reg);

        // 📩 SEND CANCEL EMAIL
        emailService.sendEventCancellationEmail(
                user.getEmail(),
                user.getFullName(),
                event,
                refund
        );

        return "Registration canceled. " + refund + " points refunded.";
    }


    // =========================================================
    // 🔹 END EVENT
    // =========================================================
    @Override
    @Transactional
    public String endEvent(CustomUserDetails principal, EventEndRequest req) {

        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getHostClub() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event must have a host club before ending.");
        }

        Wallet eventWallet = ensureEventWallet(event);

        List<EventRegistration> regs = regRepo.findByEvent_EventId(event.getEventId());
        long totalReward = 0L;

        for (EventRegistration reg : regs) {

            AttendanceLevelEnum level = Optional.ofNullable(reg.getAttendanceLevel())
                    .orElse(AttendanceLevelEnum.NONE);

            // ❌ Skip if no attendance
            if (level == AttendanceLevelEnum.NONE) continue;

            // ⚠️ Suspicious → send warning email → skip reward
            if (level == AttendanceLevelEnum.SUSPICIOUS) {

                emailService.sendSuspiciousAttendanceEmail(
                        reg.getUser().getEmail(),
                        reg.getUser().getFullName(),
                        event
                );

                continue;
            }

            long commit = Optional.ofNullable(reg.getCommittedPoints()).orElse(0);
            if (commit <= 0) continue;

            long baseReward = switch (level) {
                case HALF -> commit;
                case FULL -> 2L * commit;
                default -> 0L;
            };

            Membership membership = membershipRepo
                    .findByUser_UserIdAndClub_ClubId(
                            reg.getUser().getUserId(),
                            event.getHostClub().getClubId()
                    )
                    .orElse(null);

            if (membership == null) continue;

            double clubMultiplier = Optional.ofNullable(event.getHostClub().getClubMultiplier()).orElse(1.0);
            double memberMultiplier = Optional.ofNullable(membership.getMemberMultiplier()).orElse(1.0);
            double eventMultiplier = (event.getType() == EventTypeEnum.SPECIAL) ? 1.5 : 1.0;

            long finalReward = Math.round(baseReward * clubMultiplier * memberMultiplier * eventMultiplier);
            if (finalReward <= 0) continue;

            // 💸 Transfer reward
            Wallet memberWallet = walletService.getOrCreateUserWallet(membership.getUser());
            walletService.transferPointsWithType(
                    eventWallet, memberWallet, finalReward,
                    "Reward for " + event.getName(),
                    WalletTransactionTypeEnum.BONUS_REWARD
            );

            // 📩 Summary email
            String feedbackLink = "https://uniclub.id.vn/feedback?eventId=" + event.getEventId();

            emailService.sendEventSummaryEmail(
                    reg.getUser().getEmail(),
                    reg.getUser().getFullName(),
                    event,
                    finalReward,
                    feedbackLink
            );

            totalReward += finalReward;
            reg.setStatus(RegistrationStatusEnum.REFUNDED);
            regRepo.save(reg);
        }

        // =======================================================
        // 💰 AUTO-SETTLEMENT: chia đều leftover cho host + cohost
        // =======================================================
        rewardService.autoSettleEvent(event);

        // =======================================================
        // 🔚 CLOSE WALLET & COMPLETE EVENT
        // =======================================================
        eventWallet.setStatus(WalletStatusEnum.CLOSED);
        walletRepo.save(eventWallet);

        event.setStatus(EventStatusEnum.COMPLETED);
        event.setCompletedAt(LocalDateTime.now());
        eventRepo.save(event);

        return "Event completed. Total reward " + totalReward + " pts; leftover refunded.";
    }




    private Wallet ensureEventWallet(Event event) {
        Wallet w = event.getWallet();

        if (w == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet not found. UniStaff must approve the event budget first.");
        }

        // Ví đã bị đóng → không được phép giao dịch nữa
        if (w.getStatus() == WalletStatusEnum.CLOSED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet is already closed.");
        }

        return w;
    }

}
