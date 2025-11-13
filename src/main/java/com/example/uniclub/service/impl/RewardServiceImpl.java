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

        String formattedEarned = formatPoints(pointsEarned);
        String formattedTotal = formatPoints(totalPoints);

        emailService.sendEmail(
                user.getEmail(),
                "You’ve Earned UniPoints for Attending " + eventName + "!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Thank you for attending <b>%s</b>!</p>
                <p>You’ve just earned <b>%s UniPoints</b>.</p>
                <p>Your current total is <b>%s UniPoints</b>.</p>
                <p>Keep participating in events to earn more rewards.</p>
                """.formatted(user.getFullName(), eventName, formattedEarned, formattedTotal)
        );
    }

    @Override
    public void sendManualBonusEmail(Long userId, long bonusPoints, String reason, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        String formattedBonus = formatPoints(bonusPoints);
        String formattedTotal = formatPoints(totalPoints);
        String r = (reason == null || reason.isBlank()) ? "your contribution" : reason;

        emailService.sendEmail(
                user.getEmail(),
                "You’ve Received Extra UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Good news! You’ve received <b>%s UniPoints</b> for <b>%s</b>.</p>
                <p>Your total balance is now <b>%s UniPoints</b>.</p>
                <p>Keep up the great work and continue to shine in UniClub!</p>
                """.formatted(user.getFullName(), formattedBonus, r, formattedTotal)
        );
    }

    @Override
    public void sendMilestoneEmail(Long userId, long milestone) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        String formattedMilestone = formatPoints(milestone);

        emailService.sendEmail(
                user.getEmail(),
                "Congratulations! You’ve Reached " + formattedMilestone + " UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Congratulations on reaching <b>%s UniPoints</b>!</p>
                <p>This milestone reflects your consistent engagement with UniClub.</p>
                <p>Keep exploring more clubs and events to earn even greater rewards!</p>
                """.formatted(user.getFullName(), formattedMilestone)
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

        String subject = "[UniClub] Your club just received points 🎉";

        String formattedPoints = formatPoints(points);

        String html = """
    <h2>Hello %s,</h2>
    <p>Your club <b>%s</b> has just received <b>%s points</b>.</p>
    <p><b>Reason:</b> %s</p>
    <br>
    <p>Best regards,<br>UniClub System</p>
    """.formatted(
                user.getFullName(),
                clubName,
                formattedPoints,
                reason
        );

        emailService.sendEmail(user.getEmail(), subject, html);
    }

    public void sendClubWalletDeductionEmail(Long userId, String clubName, long points, String reason) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String subject = "[UniClub] Your club wallet has been used";

        String formattedPoints = formatPoints(points);

        String html = """
    <h2>Hello %s,</h2>
    <p>Your club <b>%s</b> has just spent <b>%s points</b> from the club wallet.</p>
    <p><b>Reason:</b> %s</p>
    <br>
    <p>If this action was not performed by you or your club's management team, please contact the university staff.</p>
    <br>
    <p>Best regards,<br>UniClub System</p>
    """.formatted(
                user.getFullName(),
                clubName,
                formattedPoints,
                reason
        );

        emailService.sendEmail(user.getEmail(), subject, html);
    }


    public void sendClubBatchDeductionSummaryEmail(Long userId, String clubName, long totalPoints, int memberCount, String reason) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String subject = "[UniClub] Club wallet used for member rewards";

        String formattedTotal = formatPoints(totalPoints);

        String html = """
    <h2>Hello %s,</h2>
    <p>Your club <b>%s</b> has just spent <b>%s points</b> from the club wallet to reward <b>%d members</b>.</p>
    <p><b>Reason:</b> %s</p>
    <br>
    <p>You can check the detailed transactions in the UniClub system.</p>
    <br>
    <p>Best regards,<br>UniClub System</p>
    """.formatted(
                user.getFullName(),
                clubName,
                formattedTotal,
                memberCount,
                reason
        );

        emailService.sendEmail(user.getEmail(), subject, html);
    }


}
