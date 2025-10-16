package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventCheckinRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventRegisterRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.EventPointsService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventPointsServiceImpl implements EventPointsService {

    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final WalletService walletService;

    // ============ REGISTER ============
    @Override
    @Transactional
    public String register(CustomUserDetails principal, EventRegisterRequest req) {
        User user = userRepo.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for registration");
        }

        if (regRepo.existsByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already registered for this event");
        }

        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The event has already ended");
        }

        Wallet userWallet = walletService.getWalletByUserId(user.getUserId());
        Wallet eventWallet = ensureEventWallet(event);

        int commitPoints = event.getCommitPointCost();
        walletService.decrease(userWallet, commitPoints);
        walletService.increase(eventWallet, commitPoints);

        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatusEnum.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .committedPoints(commitPoints)
                .build();
        regRepo.save(reg);

        return "✅ Registration successful. " + commitPoints + " commitment points have been deducted.";
    }

    // ============ CHECK-IN ============
    @Override
    @Transactional
    public String checkin(CustomUserDetails principal, EventCheckinRequest req) {
        User user = principal.getUser();
        Event event = eventRepo.findByCheckInCode(req.checkInCode())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code"));

        if (event.getStatus() != EventStatusEnum.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for check-in");
        }

        if (event.getMaxCheckInCount() != null &&
                event.getCurrentCheckInCount() >= event.getMaxCheckInCount()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event has reached full capacity");
        }

        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not registered for this event"));

        if (reg.getStatus() == RegistrationStatusEnum.CHECKED_IN) {
            return "ℹ️ You have already checked in earlier.";
        }

        AttendanceLevelEnum level = req.level();
        int capped = Math.min(level.getFactor(), event.getRewardMultiplierCap());

        reg.setAttendanceLevel(switch (capped) {
            case 1 -> AttendanceLevelEnum.X1;
            case 2 -> AttendanceLevelEnum.X2;
            default -> AttendanceLevelEnum.X3;
        });
        reg.setCheckinAt(LocalDateTime.now());
        reg.setStatus(RegistrationStatusEnum.CHECKED_IN);
        regRepo.save(reg);

        event.setCurrentCheckInCount(event.getCurrentCheckInCount() + 1);
        eventRepo.save(event);

        return "✅ Check-in successful with level " + reg.getAttendanceLevel().name()
                + ". Points will be rewarded after the event ends.";
    }

    // ============ CANCEL REGISTRATION ============
    @Override
    @Transactional
    public String cancelRegistration(CustomUserDetails principal, Long eventId) {
        User user = principal.getUser();

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(eventId, user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not registered for this event"));

        if (reg.getStatus() == RegistrationStatusEnum.CANCELED) {
            return "ℹ️ You have already canceled this registration.";
        }

        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot cancel after the event date.");
        }

        Wallet eventWallet = ensureEventWallet(event);
        Wallet userWallet = walletService.getWalletByUserId(user.getUserId());

        int refund = reg.getCommittedPoints();
        walletService.decrease(eventWallet, refund);
        walletService.increase(userWallet, refund);

        reg.setStatus(RegistrationStatusEnum.CANCELED);
        reg.setCanceledAt(LocalDateTime.now());
        regRepo.save(reg);

        return "❌ Registration canceled. " + refund + " commitment points have been refunded to your wallet.";
    }

    // ============ END EVENT ============
    @Override
    @Transactional
    public String endEvent(CustomUserDetails principal, EventEndRequest req) {
        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not in a state that can be ended");
        }

        Wallet eventWallet = ensureEventWallet(event);

        List<EventRegistration> checkedIns =
                regRepo.findByEvent_EventIdAndStatus(event.getEventId(), RegistrationStatusEnum.CHECKED_IN);

        int totalPayout = 0;
        for (EventRegistration reg : checkedIns) {
            int commit = reg.getCommittedPoints();
            int factor = reg.getAttendanceLevel() == null ? 1 : reg.getAttendanceLevel().getFactor();
            factor = Math.min(factor, event.getRewardMultiplierCap());

            int refund = commit;
            int bonus = commit * (factor - 1);
            int payout = refund + bonus;

            Wallet userWallet = walletService.getWalletByUserId(reg.getUser().getUserId());
            walletService.decrease(eventWallet, payout);
            walletService.increase(userWallet, payout);
            totalPayout += payout;

            reg.setStatus(RegistrationStatusEnum.REFUNDED);
            regRepo.save(reg);
        }

        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);

        int leftover = eventWallet.getBalancePoints();
        if (leftover > 0) {
            Wallet clubWallet = event.getClub().getWallet();
            if (clubWallet == null) {
                clubWallet = walletRepo.save(Wallet.builder()
                        .ownerType(WalletOwnerTypeEnum.CLUB)
                        .club(event.getClub())
                        .balancePoints(0)
                        .build());
                event.getClub().setWallet(clubWallet);
            }
            walletService.decrease(eventWallet, leftover);
            walletService.increase(clubWallet, leftover);
        }

        walletRepo.delete(eventWallet);
        event.setWallet(null);
        eventRepo.save(event);

        return "🏁 Event completed. Total " + totalPayout +
                " points have been distributed to participants. Remaining " + leftover +
                " points have been transferred back to the host club.";
    }

    // ============ EVENT REGISTRATIONS ============
    @Override
    @Transactional(readOnly = true)
    public List<EventRegistration> getEventRegistrations(Long eventId) {
        eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        return regRepo.findByEvent_EventId(eventId);
    }

    // ============ EVENT SUMMARY ============
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventSummary(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

        int totalCommit = regs.stream().mapToInt(r -> r.getCommittedPoints() == null ? 0 : r.getCommittedPoints()).sum();
        int refunded = (int) regs.stream().filter(r -> r.getStatus() == RegistrationStatusEnum.REFUNDED).count();
        int checkedIn = (int) regs.stream().filter(r -> r.getStatus() == RegistrationStatusEnum.CHECKED_IN).count();

        return Map.of(
                "eventName", event.getName(),
                "totalCommitPoints", totalCommit,
                "checkedInCount", checkedIn,
                "refundedCount", refunded,
                "registrationsCount", regs.size()
        );
    }

    // ============ EVENT WALLET ============
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
                "clubId", event.getClub().getClubId()
        );
    }

    // ============ UTIL ============
    private Wallet ensureEventWallet(Event event) {
        Wallet w = event.getWallet();
        if (w == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event wallet not found. Please APPROVE the event first.");
        }
        return w;
    }
}
