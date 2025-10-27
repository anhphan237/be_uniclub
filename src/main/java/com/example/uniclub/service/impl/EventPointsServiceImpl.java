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

        walletService.decrease(memberWallet, commitPoints);
        walletService.increase(eventWallet, commitPoints);

        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatusEnum.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .committedPoints(commitPoints)
                .build();
        regRepo.save(reg);

        return "✅ Registered successfully. " + commitPoints + " commitment points deducted from your membership wallet.";
    }

    // =========================================================
    // 🔹 CHECK-IN
    // =========================================================
    @Override
    @Transactional
    public String checkin(CustomUserDetails principal, EventCheckinRequest req) {
        User user = principal.getUser();

        Event event = eventRepo.findByCheckInCode(req.checkInCode())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invalid check-in code"));

        if (event.getStatus() != EventStatusEnum.APPROVED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for check-in");

        if (event.getMaxCheckInCount() != null
                && event.getCurrentCheckInCount() >= event.getMaxCheckInCount())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event has reached full capacity");

        EventRegistration reg = regRepo.findByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not registered for this event"));

        if (reg.getStatus() == RegistrationStatusEnum.CHECKED_IN)
            return "ℹ️ You have already checked in earlier.";

        AttendanceLevelEnum level = req.level();
        int capped = Math.min(level.getFactor(), event.getRewardMultiplierCap());
        reg.setAttendanceLevel(
                switch (capped) {
                    case 1 -> AttendanceLevelEnum.X1;
                    case 2 -> AttendanceLevelEnum.X2;
                    default -> AttendanceLevelEnum.X3;
                }
        );

        reg.setCheckinAt(LocalDateTime.now());
        reg.setStatus(RegistrationStatusEnum.CHECKED_IN);
        regRepo.save(reg);

        event.setCurrentCheckInCount(event.getCurrentCheckInCount() + 1);
        eventRepo.save(event);

        return "✅ Check-in successful with level " + reg.getAttendanceLevel().name()
                + ". Points will be rewarded after the event ends.";
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

        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot cancel after event date.");

        Club hostClub = event.getHostClub();
        Membership membership = membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), hostClub.getClubId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found in host club"));

        Wallet memberWallet = walletService.getOrCreateMembershipWallet(membership);
        Wallet eventWallet = ensureEventWallet(event);

        int refund = reg.getCommittedPoints();
        walletService.decrease(eventWallet, refund);
        walletService.increase(memberWallet, refund);

        reg.setStatus(RegistrationStatusEnum.CANCELED);
        reg.setCanceledAt(LocalDateTime.now());
        regRepo.save(reg);

        return "❌ Registration canceled. " + refund + " points refunded to your membership wallet.";
    }

    // =========================================================
    // 🔹 END EVENT
    // =========================================================
    @Override
    @Transactional
    public String endEvent(CustomUserDetails principal, EventEndRequest req) {
        Event event = eventRepo.findById(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.APPROVED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event not active");

        Wallet eventWallet = ensureEventWallet(event);
        List<EventRegistration> checkedIns = regRepo.findByEvent_EventIdAndStatus(event.getEventId(), RegistrationStatusEnum.CHECKED_IN);

        int totalPayout = 0;
        for (EventRegistration reg : checkedIns) {
            Club hostClub = event.getHostClub();
            Membership membership = membershipRepo.findByUser_UserIdAndClub_ClubId(
                            reg.getUser().getUserId(), hostClub.getClubId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found for participant"));

            Wallet memberWallet = walletService.getOrCreateMembershipWallet(membership);

            int commit = reg.getCommittedPoints();
            int factor = reg.getAttendanceLevel() == null ? 1 : reg.getAttendanceLevel().getFactor();
            factor = Math.min(factor, event.getRewardMultiplierCap());
            int payout = commit * factor;

            walletService.decrease(eventWallet, payout);
            walletService.increase(memberWallet, payout);
            totalPayout += payout;

            reg.setStatus(RegistrationStatusEnum.REFUNDED);
            regRepo.save(reg);
        }

        event.setStatus(EventStatusEnum.COMPLETED);
        eventRepo.save(event);

        // 🔸 Expire staff
        List<EventStaff> staffs = eventStaffRepo.findByEvent_EventId(event.getEventId());
        for (EventStaff s : staffs) {
            if (s.getState() == EventStaffStateEnum.ACTIVE) {
                s.setState(EventStaffStateEnum.EXPIRED);
                s.setUnassignedAt(LocalDateTime.now());
            }
        }
        eventStaffRepo.saveAll(staffs);

        // 🔸 Distribute leftover
        int leftover = eventWallet.getBalancePoints().intValue();
        if (leftover > 0) {
            List<Club> clubsToReward = event.getCoHostedClubs();
            if (clubsToReward == null || clubsToReward.isEmpty())
                clubsToReward = new ArrayList<>();
            if (event.getHostClub() != null && !clubsToReward.contains(event.getHostClub()))
                clubsToReward.add(event.getHostClub());

            int share = leftover / clubsToReward.size();
            for (Club club : clubsToReward) {
                Wallet clubWallet = walletService.getOrCreateClubWallet(club);
                walletService.decrease(eventWallet, share);
                walletService.increase(clubWallet, share);
            }
        }


        walletRepo.delete(eventWallet);
        event.setWallet(null);
        eventRepo.save(event);

        return "🏁 Event completed: " + totalPayout + " points rewarded; remaining "
                + leftover + " shared among host/co-host.";
    }

    // =========================================================
    // 🔹 UTIL
    // =========================================================
    private Wallet ensureEventWallet(Event event) {
        Wallet w = event.getWallet();
        if (w == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event wallet not found. Please APPROVE the event first.");
        return w;
    }

    // =========================================================
    // 🔹 EVENT REGISTRATIONS
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public List<EventRegistration> getEventRegistrations(Long eventId) {
        eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        return regRepo.findByEvent_EventId(eventId);
    }

    // =========================================================
    // 🔹 EVENT SUMMARY
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventSummary(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        List<EventRegistration> regs = regRepo.findByEvent_EventId(eventId);

        int totalCommit = regs.stream()
                .mapToInt(r -> r.getCommittedPoints() == null ? 0 : r.getCommittedPoints())
                .sum();
        long refunded = regs.stream()
                .filter(r -> r.getStatus() == RegistrationStatusEnum.REFUNDED)
                .count();
        long checkedIn = regs.stream()
                .filter(r -> r.getStatus() == RegistrationStatusEnum.CHECKED_IN)
                .count();

        return Map.<String, Object>of(
                "eventName", event.getName(),
                "totalCommitPoints", totalCommit,
                "checkedInCount", checkedIn,
                "refundedCount", refunded,
                "registrationsCount", regs.size()
        );
    }

    // =========================================================
    // 🔹 EVENT WALLET INFO
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getEventWallet(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        Wallet wallet = ensureEventWallet(event);

        return Map.<String, Object>of(
                "eventId", event.getEventId(),
                "eventName", event.getName(),
                "walletBalance", wallet.getBalancePoints(),
                "ownerType", wallet.getOwnerType().name(),
                "hostClubId", event.getHostClub().getClubId()
        );
    }
}
