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
        Wallet eventWallet = event.getWallet();
        if (eventWallet == null) {
            log.warn("Event '{}' has no wallet, skipping settlement.", event.getName());
            return;
        }

        long budget = event.getBudgetPoints();
        long remaining = eventWallet.getBalancePoints(); // lấy trực tiếp điểm còn dư thực tế

        if (remaining <= 0) {
            log.info("No remaining points to refund for event '{}'.", event.getName());
            return;
        }

        // 1️⃣ Giảm điểm dư khỏi ví sự kiện
        eventWallet.setBalancePoints(0L);
        walletRepo.save(eventWallet);

        // 2️⃣ Xác định các CLB được hoàn (host + co-host APPROVED)
        List<Wallet> destinationWallets = new ArrayList<>();

        if (event.getHostClub() != null) {
            Wallet hostWallet = walletRepo.findByClub_ClubId(event.getHostClub().getClubId())
                    .orElseGet(() -> {
                        Wallet w = Wallet.builder()
                                .club(event.getHostClub())
                                .ownerType(WalletOwnerTypeEnum.CLUB)
                                .balancePoints(0L)
                                .build();
                        walletRepo.save(w);
                        return w;
                    });
            destinationWallets.add(hostWallet);
        }

        if (event.getCoHostRelations() != null) {
            event.getCoHostRelations().stream()
                    .filter(rel -> rel.getStatus() == EventCoHostStatusEnum.APPROVED)
                    .map(EventCoClub::getClub)
                    .forEach(c -> {
                        Wallet coWallet = walletRepo.findByClub_ClubId(c.getClubId())
                                .orElseGet(() -> {
                                    Wallet w = Wallet.builder()
                                            .club(c)
                                            .ownerType(WalletOwnerTypeEnum.CLUB)
                                            .balancePoints(0L)
                                            .build();
                                    walletRepo.save(w);
                                    return w;
                                });
                        destinationWallets.add(coWallet);
                    });
        }

        if (destinationWallets.isEmpty()) {
            log.warn("No destination wallets found for refund in event '{}'.", event.getName());
            return;
        }

        // 3️⃣ Chia đều phần điểm dư
        long perWalletRefund = remaining / destinationWallets.size();

        for (Wallet destWallet : destinationWallets) {
            destWallet.setBalancePoints(destWallet.getBalancePoints() + perWalletRefund);
            walletRepo.save(destWallet);

            // 4️⃣ Ghi lịch sử giao dịch
            WalletTransaction tx = WalletTransaction.builder()
                    .wallet(destWallet)
                    .amount(perWalletRefund)
                    .type(WalletTransactionTypeEnum.RETURN_SURPLUS)
                    .description(String.format(
                            "Refund %,d points from event '%s' (shared remaining budget)",
                            perWalletRefund, event.getName()))
                    .senderName(event.getName())
                    .receiverName(destWallet.getDisplayName())
                    .build();

            txRepo.save(tx);
        }

        log.info("✅ {} points refunded from event '{}' to {} club wallet(s).",
                remaining, event.getName(), destinationWallets.size());
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
