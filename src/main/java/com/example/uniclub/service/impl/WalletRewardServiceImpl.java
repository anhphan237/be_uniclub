package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.WalletRewardService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletRewardServiceImpl implements WalletRewardService {

    private final WalletService walletService;
    private final MembershipRepository membershipRepo;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;
    private final EmailService emailService;

    // ==============================================================
    // 🔹 Hệ số member
    // ==============================================================
    private double getMemberMultiplier(User user) {
        return membershipRepo.findActiveMembershipsByUserId(user.getUserId())
                .stream()
                .map(Membership::getMemberMultiplier)
                .max(Double::compare)
                .orElse(1.0);
    }

    // ==============================================================
    // 🔹 Hệ số CLB
    // ==============================================================
    private double getClubMultiplier(Club club) {
        Double m = club.getClubMultiplier();
        return (m == null || m <= 0) ? 1.0 : m;
    }

    // ==============================================================
    // 🎁 1️⃣ Thưởng cho 1 user
    // ==============================================================
    @Override
    @Transactional
    public Wallet rewardPointsByUser(User operator, Long userId, long points, String reason) {

        String role = operator.getRole().getRoleName();
        boolean isAdmin = role.equalsIgnoreCase("ADMIN");
        boolean isStaff = role.equalsIgnoreCase("UNIVERSITY_STAFF");
        boolean isLeader = role.equalsIgnoreCase("CLUB_LEADER");
        boolean isVice = role.equalsIgnoreCase("VICE_LEADER");

        if (!(isAdmin || isStaff || isLeader || isVice))
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to reward points.");

        User targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        double memberMultiplier = getMemberMultiplier(targetUser);
        long finalPoints = Math.round(points * memberMultiplier);
        String finalReason = (reason == null ? "Manual reward" : reason);

        // =========== Leader/Vice thưởng từ ví CLB ===========
        if (isLeader || isVice) {

            List<Membership> operatorMemberships =
                    membershipRepo.findByUser_UserId(operator.getUserId());

            if (operatorMemberships.isEmpty())
                throw new ApiException(HttpStatus.FORBIDDEN, "You belong to no club.");

            Club club = operatorMemberships.get(0).getClub();
            Wallet clubWallet = walletService.getOrCreateClubWallet(club);
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            if (clubWallet.getBalancePoints() < finalPoints)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

            walletService.transferPointsWithType(
                    clubWallet,
                    userWallet,
                    finalPoints,
                    finalReason,
                    WalletTransactionTypeEnum.CLUB_TO_MEMBER
            );

            // 🎯 Gửi email cho member
            emailService.sendManualBonusEmail(
                    targetUser.getEmail(),
                    targetUser.getFullName(),
                    finalPoints,
                    finalReason,
                    userWallet.getBalancePoints()
            );

            // 🎯 Gửi email leader/vice về việc trừ ví CLB
            membershipRepo.findByClub_ClubId(club.getClubId())
                    .stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER
                            || m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .forEach(m ->
                            emailService.sendClubWalletDeductionEmail(
                                    m.getUser().getEmail(),
                                    m.getUser().getFullName(),
                                    club.getName(),
                                    finalPoints,
                                    finalReason
                            )
                    );

            return userWallet;
        }

        // =========== Admin / Staff thưởng trực tiếp ===========
        Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

        walletService.increase(userWallet, finalPoints);
        walletService.logTransactionFromSystem(
                userWallet,
                finalPoints,
                WalletTransactionTypeEnum.ADD,
                finalReason
        );

        // 🎯 Email thưởng cho member
        emailService.sendManualBonusEmail(
                targetUser.getEmail(),
                targetUser.getFullName(),
                finalPoints,
                finalReason,
                userWallet.getBalancePoints()
        );

        return userWallet;
    }

    // ==============================================================
    // 🏛 2️⃣ Top-up điểm cho Club
    // ==============================================================
    @Override
    @Transactional
    public Wallet topUpClubWallet(User operator, Long clubId, long points, String reason) {

        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN, "Only staff or admin can top up club wallets.");

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found."));

        Wallet clubWallet = walletService.getOrCreateClubWallet(club);

        walletService.topupPointsFromUniversityWithOperator(
                clubWallet.getWalletId(),
                points,
                (reason == null ? "Top-up by university staff" : reason),
                operator.getFullName()
        );

        // 🎯 Email cho leader & vice
        membershipRepo.findByClub_ClubId(clubId)
                .stream()
                .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                .forEach(m ->
                        emailService.sendClubTopUpEmail(
                                m.getUser().getEmail(),
                                m.getUser().getFullName(),
                                club.getName(),
                                points,
                                (reason == null ? "Top-up by university staff" : reason)
                        )
                );

        return clubWallet;
    }

    // ==============================================================
    // 🏆 3️⃣ Thưởng hàng loạt cho nhiều CLB
    // ==============================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleClubs(WalletRewardBatchRequest req) {

        List<WalletTransactionResponse> responses = new ArrayList<>();

        for (Long clubId : req.getTargetIds()) {

            Club club = clubRepo.findById(clubId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Club not found: " + clubId));

            double clubMultiplier = getClubMultiplier(club);
            long finalPoints = Math.round(req.getPoints() * clubMultiplier);

            Wallet clubWallet = walletService.getOrCreateClubWallet(club);
            WalletTransaction tx = walletService.topupPointsFromUniversity(
                    clubWallet,
                    finalPoints,
                    req.getReason() + " (x" + clubMultiplier + ")"
            );

            // 🎯 Email leader/vice
            membershipRepo.findByClub_ClubId(clubId)
                    .stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                            m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .forEach(m ->
                            emailService.sendClubTopUpEmail(
                                    m.getUser().getEmail(),
                                    m.getUser().getFullName(),
                                    club.getName(),
                                    finalPoints,
                                    req.getReason() + " (x" + clubMultiplier + ")"
                            )
                    );

            responses.add(WalletTransactionResponse.builder()
                    .id(tx.getId())
                    .type(tx.getType().name())
                    .amount(tx.getAmount())
                    .signedAmount("+" + tx.getAmount())
                    .description(tx.getDescription())
                    .senderName(tx.getSenderName())
                    .receiverName(club.getName())
                    .createdAt(tx.getCreatedAt())
                    .build());
        }

        return responses;
    }

    // ==============================================================
    // 👥 4️⃣ Thưởng nhiều members
    // ==============================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleMembers(User operator, WalletRewardBatchRequest req) {

        List<WalletTransactionResponse> responses = new ArrayList<>();

        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");
        boolean isLeaderOrVice = role.equalsIgnoreCase("CLUB_LEADER") || role.equalsIgnoreCase("VICE_LEADER");

        // ==============================================================
        //  A) ADMIN / STAFF thưởng trực tiếp
        // ==============================================================
        if (isAdminOrStaff) {

            for (Long userId : req.getTargetIds()) {

                User u = userRepo.findById(userId)
                        .orElseThrow(() ->
                                new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

                double memberMultiplier = getMemberMultiplier(u);
                long finalPoints = Math.round(req.getPoints() * memberMultiplier);

                Wallet userWallet = walletService.getOrCreateUserWallet(u);

                walletService.increase(userWallet, finalPoints);
                walletService.logTransactionFromSystem(
                        userWallet,
                        finalPoints,
                        WalletTransactionTypeEnum.ADD,
                        req.getReason() + " (x" + memberMultiplier + ")"
                );

                // 🎯 Email thưởng
                emailService.sendManualBonusEmail(
                        u.getEmail(),
                        u.getFullName(),
                        finalPoints,
                        req.getReason(),
                        userWallet.getBalancePoints()
                );

                responses.add(WalletTransactionResponse.builder()
                        .type("UNI_TO_MEMBER")
                        .amount(finalPoints)
                        .signedAmount("+" + finalPoints)
                        .description(req.getReason() + " (x" + memberMultiplier + ")")
                        .senderName("University System")
                        .receiverName(u.getFullName())
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            return responses;
        }

        // ==============================================================
        //  B) LEADER / VICE thưởng member trong CLB mình
        // ==============================================================
        if (isLeaderOrVice) {

            List<Membership> memberships = membershipRepo.findByUser_UserId(operator.getUserId());
            if (memberships.isEmpty())
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not in any club.");

            Club club = memberships.get(0).getClub();
            Wallet clubWallet = walletService.getOrCreateClubWallet(club);

            List<Long> realMemberIds =
                    membershipRepo.findByClub_ClubId(club.getClubId())
                            .stream()
                            .filter(m -> m.getClubRole() == ClubRoleEnum.MEMBER)
                            .filter(m -> m.getState() == MembershipStateEnum.APPROVED ||
                                    m.getState() == MembershipStateEnum.ACTIVE)
                            .map(m -> m.getUser().getUserId())
                            .filter(req.getTargetIds()::contains)
                            .toList();

            if (realMemberIds.isEmpty())
                throw new ApiException(HttpStatus.BAD_REQUEST, "No valid members to reward.");

            double clubMultiplier = getClubMultiplier(club);
            long totalSpent = 0;
            int rewardCount = 0;

            for (Long memberId : realMemberIds) {

                User target = userRepo.findById(memberId)
                        .orElseThrow(() ->
                                new ApiException(HttpStatus.NOT_FOUND, "User not found: " + memberId));

                double memberMultiplier = getMemberMultiplier(target);
                long finalPoints = Math.round(req.getPoints() * memberMultiplier * clubMultiplier);

                if (clubWallet.getBalancePoints() < finalPoints)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

                Wallet userWallet = walletService.getOrCreateUserWallet(target);

                walletService.transferPointsWithType(
                        clubWallet,
                        userWallet,
                        finalPoints,
                        req.getReason() + String.format(" (x%.2f×%.2f)", memberMultiplier, clubMultiplier),
                        WalletTransactionTypeEnum.CLUB_TO_MEMBER
                );

                // 🎯 Email thưởng member
                emailService.sendManualBonusEmail(
                        target.getEmail(),
                        target.getFullName(),
                        finalPoints,
                        req.getReason(),
                        userWallet.getBalancePoints()
                );

                responses.add(WalletTransactionResponse.builder()
                        .type("CLUB_TO_MEMBER")
                        .amount(finalPoints)
                        .signedAmount("+" + finalPoints)
                        .description(req.getReason() + String.format(" (x%.2f×%.2f)", memberMultiplier, clubMultiplier))
                        .senderName(club.getName())
                        .receiverName(target.getFullName())
                        .createdAt(LocalDateTime.now())
                        .build());

                totalSpent += finalPoints;
                rewardCount++;
            }

            long totalSpentFinal = totalSpent;
            int rewardCountFinal = rewardCount;

            // 🎯 Email tổng kết gửi leader + vice
            membershipRepo.findByClub_ClubId(club.getClubId())
                    .stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                            m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .forEach(m ->
                            emailService.sendClubBatchDeductionSummaryEmail(
                                    m.getUser().getEmail(),
                                    m.getUser().getFullName(),
                                    club.getName(),
                                    totalSpentFinal,
                                    rewardCountFinal,
                                    req.getReason()
                            )
                    );
        }

        return responses;
    }

}
