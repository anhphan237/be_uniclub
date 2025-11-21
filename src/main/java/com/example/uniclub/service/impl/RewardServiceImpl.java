package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    private final EventRepository eventRepo;

    private final EmailService emailService;
    private final UserRepository userRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;

    // =====================================================
    // 🧩 EMAIL REWARD NOTIFICATIONS
    // =====================================================

    private String formatPoints(long points) {
        return NumberFormat.getNumberInstance(Locale.US)
                .format(points)
                .replace(",", ".");
    }


    @Override
    public void sendCheckInRewardEmail(Long userId, String eventName, long pointsEarned, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        emailService.sendCheckInRewardEmail(
                user.getEmail(),
                user.getFullName(),
                eventName,
                pointsEarned,
                totalPoints
        );
    }

    @Override
    public void sendManualBonusEmail(Long userId, long bonusPoints, String reason, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        emailService.sendManualBonusEmail(
                user.getEmail(),
                user.getFullName(),
                bonusPoints,
                reason,
                totalPoints
        );
    }

    @Override
    public void sendMilestoneEmail(Long userId, long milestone) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        emailService.sendMilestoneEmail(
                user.getEmail(),
                user.getFullName(),
                milestone
        );
    }

    // =====================================================
    // 💰 AUTO SETTLEMENT: HOÀN ĐIỂM DƯ VỀ CLB
    // =====================================================

    @Transactional
    @Override
    public void autoSettleEvent(Event event) {

        // 🔥 Load full event with co-host relations
        Event eventFull = eventRepo.findByIdWithCoHostRelations(event.getEventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        Wallet eventWallet = eventFull.getWallet();
        if (eventWallet == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Event wallet missing. Cannot settle event.");
        }

        if (eventWallet.getStatus() == WalletStatusEnum.CLOSED) {
            log.warn("Event wallet already closed → skip settlement.");
            return;
        }

        long remaining = eventWallet.getBalancePoints();
        if (remaining <= 0) {
            log.info("No remaining points to refund for event '{}'.", eventFull.getName());
            return;
        }

        // ============================================
        // 🔹 1) Build list of destination club wallets
        // ============================================
        List<Wallet> destinationWallets = new ArrayList<>();

        // --- HOST CLUB ---
        Wallet hostWallet = walletRepo.findByClub_ClubId(eventFull.getHostClub().getClubId())
                .orElseGet(() -> walletRepo.save(
                        Wallet.builder()
                                .club(eventFull.getHostClub())
                                .ownerType(WalletOwnerTypeEnum.CLUB)
                                .balancePoints(0L)
                                .status(WalletStatusEnum.ACTIVE)
                                .build()
                ));
        destinationWallets.add(hostWallet);

        // --- CO-HOST CLUBS (APPROVED ONLY) ---
        List<EventCoClub> rels = Optional.ofNullable(eventFull.getCoHostRelations()).orElse(List.of());
        for (EventCoClub rel : rels) {
            if (rel.getStatus() != EventCoHostStatusEnum.APPROVED) continue;

            Club coClub = rel.getClub();
            if (coClub == null) continue;

            Wallet coWallet = walletRepo.findByClub_ClubId(coClub.getClubId())
                    .orElseGet(() -> walletRepo.save(
                            Wallet.builder()
                                    .club(coClub)
                                    .ownerType(WalletOwnerTypeEnum.CLUB)
                                    .balancePoints(0L)
                                    .status(WalletStatusEnum.ACTIVE)
                                    .build()
                    ));

            destinationWallets.add(coWallet);
        }

        if (destinationWallets.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No club wallets found to receive leftover points.");
        }

        // ============================================
        // 🔹 2) Split leftover evenly + host gets remainder
        // ============================================
        int walletCount = destinationWallets.size();
        long baseRefund = remaining / walletCount;
        long leftover = remaining % walletCount;

        for (int i = 0; i < walletCount; i++) {
            Wallet w = destinationWallets.get(i);

            long refundAmount = baseRefund + (i == 0 ? leftover : 0);

            w.setBalancePoints(w.getBalancePoints() + refundAmount);
            walletRepo.save(w);

            txRepo.save(WalletTransaction.builder()
                    .wallet(w)
                    .amount(refundAmount)
                    .type(WalletTransactionTypeEnum.RETURN_SURPLUS)
                    .description("Refund " + refundAmount + " leftover points from event '" + eventFull.getName() + "'")
                    .senderName(eventFull.getName())
                    .receiverName(w.getDisplayName())
                    .build());
        }

        // ============================================
        // 🔹 3) Reset event wallet to 0
        // ============================================
        eventWallet.setBalancePoints(0L);
        walletRepo.save(eventWallet);

        log.info("🎉 Settlement COMPLETE for event '{}' – {} pts refunded to {} club wallets.",
                eventFull.getName(), remaining, walletCount);
    }




    public void sendClubTopUpEmail(Long userId, String clubName, long points, String reason) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        emailService.sendClubTopUpEmail(
                user.getEmail(),
                user.getFullName(),
                clubName,
                points,
                reason
        );
    }

    public void sendClubWalletDeductionEmail(Long userId, String clubName, long points, String reason) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        emailService.sendClubWalletDeductionEmail(
                user.getEmail(),
                user.getFullName(),
                clubName,
                points,
                reason
        );
    }


    public void sendClubBatchDeductionSummaryEmail(Long userId, String clubName, long totalPoints, int memberCount, String reason) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        emailService.sendClubBatchDeductionSummaryEmail(
                user.getEmail(),
                user.getFullName(),
                clubName,
                totalPoints,
                memberCount,
                reason
        );
    }


}
