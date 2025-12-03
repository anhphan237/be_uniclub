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
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPointsServiceImpl implements EventPointsService {
    private final WalletTransactionRepository walletTransactionRepo;
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

        // ❌ PUBLIC không cần đăng ký
        if (event.getType() == EventTypeEnum.PUBLIC) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Public events do not require registration.");
        }

        // ❌ Event không ở trạng thái mở đăng ký
        if (!(event.getStatus() == EventStatusEnum.APPROVED
                || event.getStatus() == EventStatusEnum.ONGOING)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event is not open for registration.");
        }

        // ⭐ Lấy earliestDay và latestDay (multi-day logic)
        EventDay earliestDay = event.getDays().stream()
                .sorted(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Event days missing"));

        EventDay latestDay = event.getDays().stream()
                .max(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getEndTime))
                .orElseThrow();

        LocalDate today = LocalDate.now();

        // ❌ Event đã kết thúc (multi-day check)
        if (latestDay.getDate().isBefore(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The event has already ended.");
        }

        // ❌ Không được đăng ký khi event đã bắt đầu
        LocalDateTime eventStart = LocalDateTime.of(
                earliestDay.getDate(),
                earliestDay.getStartTime()
        );

        if (LocalDateTime.now().isAfter(eventStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This event has already started.");
        }


        // ❌ Deadline quá hạn
        if (event.getRegistrationDeadline() != null
                && today.isAfter(event.getRegistrationDeadline())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Registration deadline has passed.");
        }

        // ❌ Không cho đăng ký trùng
        if (regRepo.existsByEvent_EventIdAndUser_UserId(event.getEventId(), user.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already registered for this event.");
        }

        // 🔐 PRIVATE = chỉ member CLB chủ trì mới được đăng ký
        if (event.getType() == EventTypeEnum.PRIVATE) {
            boolean isHostMember = membershipRepo
                    .existsByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId());

            if (!isHostMember) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Private event: only members of the host club can register.");
            }
        }

        // 🤝 SPECIAL = member host hoặc member cohost
        if (event.getType() == EventTypeEnum.SPECIAL) {

            boolean isMemberHost = membershipRepo
                    .existsByUser_UserIdAndClub_ClubId(user.getUserId(), event.getHostClub().getClubId());

            boolean isMemberCoHost = event.getCoHostRelations().stream()
                    .anyMatch(rel ->
                            membershipRepo.existsByUser_UserIdAndClub_ClubId(
                                    user.getUserId(),
                                    rel.getClub().getClubId()
                            )
                    );

            if (!isMemberHost && !isMemberCoHost) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "You must be a host or co-host club member to join this event.");
            }
        }

        // 🪙 Trừ commit point
        Wallet userWallet = walletService.getOrCreateUserWallet(user);
        Wallet eventWallet = ensureEventWallet(event);

        long commitPoint = Optional.ofNullable(event.getCommitPointCost()).orElse(0);

        if (userWallet.getBalancePoints() < commitPoint) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Not enough points to register.");
        }

        walletService.transferPointsWithType(
                userWallet, eventWallet, commitPoint,
                "Register event " + event.getName(),
                WalletTransactionTypeEnum.COMMIT_LOCK
        );

        // 💾 Lưu registration
        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(RegistrationStatusEnum.CONFIRMED)
                .registeredAt(LocalDateTime.now())
                .committedPoints((int) commitPoint)
                .attendanceLevel(AttendanceLevelEnum.NONE)
                .build();

        regRepo.save(registration);

        // 📧 Email confirm — MULTI-DAY VERSION
        emailService.sendEventRegistrationEmail(
                user.getEmail(),
                user.getFullName(),
                event,
                commitPoint
        );

        return "Registered successfully. " + commitPoint + " commitment points locked.";
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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = principal.getUser();

        if (event.getType() == EventTypeEnum.PUBLIC) {
            attendanceService.handlePublicCheckin(user, event);
            eventLogService.logAction(
                    user.getUserId(),
                    user.getFullName(),
                    event.getEventId(),
                    event.getName(),
                    UserActionEnum.CHECKIN_EVENT,
                    "User performed PUBLIC check-in"
            );
            return "✅ Checked in successfully for PUBLIC event: " + event.getName();
        }

        switch (req.getLevel().toUpperCase()) {
            case "START" -> attendanceService.handleStartCheckin(user, event);
            case "MID" -> attendanceService.handleMidCheckin(user, event);
            case "END" -> attendanceService.handleEndCheckout(user, event);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid check-in phase: " + req.getLevel());
        }

        eventLogService.logAction(
                user.getUserId(), user.getFullName(),
                event.getEventId(), event.getName(),
                req.getLevel().equalsIgnoreCase("END") ? UserActionEnum.CHECKOUT_EVENT : UserActionEnum.CHECKIN_EVENT,
                "User performed " + req.getLevel().toUpperCase() + " check-in"
        );

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

        // ❌ Event canceled → user không cần hủy
        if (event.getStatus() == EventStatusEnum.CANCELLED) {
            return "Event was cancelled by the host. Your points were refunded automatically if applicable.";
        }

        // ❌ Already canceled
        if (reg.getStatus() == RegistrationStatusEnum.CANCELED) {
            return "Registration already canceled.";
        }

        // ============================================================
        // 🔥 MULTI-DAY: LẤY NGÀY BẮT ĐẦU
        // ============================================================
        EventDay earliestDay = event.getDays().stream()
                .sorted(Comparator
                        .comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Event has no days"));

        LocalDate eventStartDate = earliestDay.getDate();
        LocalTime eventStartTime = earliestDay.getStartTime();
        LocalDateTime eventStartDateTime = LocalDateTime.of(eventStartDate, eventStartTime);
        LocalDateTime now = LocalDateTime.now();

        // ❌ Không được hủy nếu event đã bắt đầu
        if (now.isAfter(eventStartDateTime)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot cancel registration after event has started.");
        }

        // ❌ Không hủy được nếu đã check-in
        if (reg.getAttendanceLevel() != null && reg.getAttendanceLevel() != AttendanceLevelEnum.NONE) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You cannot cancel because you have already checked in.");
        }

        // ============================================================
        // 🔥 KHÔNG REFUND COMMIT POINT CHO REGISTRATION CANCELED
        // ============================================================
        long committed = Optional.ofNullable(reg.getCommittedPoints())
                .map(Integer::longValue)
                .orElse(0L);

        // 🔄 Update status
        reg.setStatus(RegistrationStatusEnum.CANCELED);
        reg.setCancelledAt(LocalDateTime.now());
        regRepo.save(reg);

        // 📧 Email NO-REFUND (multi-day version)
        emailService.sendEventCancellationEmail(
                user.getEmail(),
                user.getFullName(),
                event,          // email tự lấy range ngày
                0               // refund = 0
        );

        return "Registration cancelled. Commitment points will not be refunded.";
    }


    // =========================================================
    // 🔹 END EVENT (final fixed version)
    // =========================================================
    @Override
    @Transactional
    public String endEvent(CustomUserDetails principal, EventEndRequest req) {

        Event event = eventRepo.findByIdWithCoHostRelations(req.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getHostClub() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Event must have a host club before ending.");

        Wallet eventWallet = ensureEventWallet(event);

        List<EventRegistration> regs = regRepo.findByEvent_EventId(event.getEventId());
        long totalReward = 0L;

        for (EventRegistration reg : regs) {

            AttendanceLevelEnum level = Optional.ofNullable(reg.getAttendanceLevel())
                    .orElse(AttendanceLevelEnum.NONE);

            long commit = Optional.ofNullable(reg.getCommittedPoints())
                    .map(Integer::longValue)
                    .orElse(0L);

            // ❌ Không commit → không thưởng
            if (commit <= 0) {
                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
                regRepo.save(reg);
                continue;
            }

            // ❌ SUSPICIOUS → không thưởng + gửi email
            if (level == AttendanceLevelEnum.SUSPICIOUS) {
                emailService.sendSuspiciousAttendanceEmail(
                        reg.getUser().getEmail(),
                        reg.getUser().getFullName(),
                        event
                );

                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
                regRepo.save(reg);
                continue;
            }

            // ❌ NONE → không thưởng
            if (level == AttendanceLevelEnum.NONE) {
                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
                regRepo.save(reg);
                continue;
            }

            // 🎯 Attendance factor (only commit points)
            double attendanceFactor = (level == AttendanceLevelEnum.FULL) ? 2.0 : 1.0;

            // 🎯 Reward = commit * attendance factor
            long finalReward = Math.round(commit * attendanceFactor);

            if (finalReward > 0) {
                Wallet memberWallet = walletService.getOrCreateUserWallet(reg.getUser());

                // 💰 chuyển điểm thưởng
                walletService.transferPointsWithType(
                        eventWallet, memberWallet, finalReward,
                        "Event reward for " + event.getName(),
                        WalletTransactionTypeEnum.BONUS_REWARD
                );

                // 📧 gửi email tóm tắt
                emailService.sendEventSummaryEmail(
                        reg.getUser().getEmail(),
                        reg.getUser().getFullName(),
                        event,
                        finalReward,
                        "https://uniclub.id.vn/feedback?eventId=" + event.getEventId()
                );

                reg.setStatus(RegistrationStatusEnum.REWARDED);
            } else {
                reg.setStatus(RegistrationStatusEnum.NO_SHOW);
            }

            regRepo.save(reg);
            totalReward += finalReward;
        }

        // Flush
        walletRepo.flush();
        regRepo.flush();

        // Reload event
        Event refreshed = eventRepo.findByIdWithCoHostRelations(event.getEventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event disappeared"));

        // Hoàn leftover
        rewardService.autoSettleEvent(refreshed);

        // Đóng ví sự kiện
        Wallet refreshedWallet = refreshed.getWallet();
        refreshedWallet.setStatus(WalletStatusEnum.CLOSED);
        walletRepo.save(refreshedWallet);

        refreshed.setStatus(EventStatusEnum.COMPLETED);
        refreshed.setCompletedAt(LocalDateTime.now());
        eventRepo.save(refreshed);

        return "Event completed. Total reward " + totalReward + " pts; leftover refunded.";
    }

    private Wallet ensureEventWallet(Event event) {
        Wallet w = event.getWallet();
        if (w == null)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet not found. UniStaff must approve the event budget first.");

        if (w.getStatus() == WalletStatusEnum.CLOSED)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event wallet is already closed.");

        return w;
    }

    @Override
    public void refundCommitPoints(User user, long points, Event event) {
        Wallet wallet = walletRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);

        walletTransactionRepo.save(
                WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(points)
                        .type(WalletTransactionTypeEnum.REFUND_COMMIT)
                        .description("Refund commit points from cancelled event: " + event.getName())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

}
