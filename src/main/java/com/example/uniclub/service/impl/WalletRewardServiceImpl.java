package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.RewardService;
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
    private final RewardService rewardService;
    private final UserRepository userRepo;
    private final ClubRepository clubRepo;

    // ==============================================================
    // 🔹 LẤY HỆ SỐ THÀNH VIÊN (tính từ tất cả CLB user đang tham gia)
    // ==============================================================
    private double getMemberMultiplier(User user) {
        return membershipRepo.findActiveMembershipsByUserId(user.getUserId())
                .stream()
                .map(Membership::getMemberMultiplier)
                .max(Double::compare)
                .orElse(1.0);
    }

    // ==============================================================
    // 🔹 LẤY HỆ SỐ CLB (clubMultiplier)
    // ==============================================================
    private double getClubMultiplier(Club club) {
        Double m = club.getClubMultiplier();
        return (m == null || m <= 0) ? 1.0 : m;
    }

    // ==============================================================
    // 🎁 1️⃣ THƯỞNG CHO MỘT USER
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

        // ========= Leader / Vice thưởng từ ví CLB ==========
        if (isLeader || isVice) {

            List<Membership> operatorMemberships =
                    membershipRepo.findByUser_UserId(operator.getUserId());

            if (operatorMemberships.isEmpty()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You belong to no club.");
            }

            Club club = operatorMemberships.get(0).getClub();
            Wallet clubWallet = walletService.getOrCreateClubWallet(club);
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            if (clubWallet.getBalancePoints() < finalPoints)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

            // chuyển điểm
            walletService.transferPointsWithType(
                    clubWallet,
                    userWallet,
                    finalPoints,
                    finalReason,
                    WalletTransactionTypeEnum.CLUB_TO_MEMBER
            );

            // email cho member
            rewardService.sendManualBonusEmail(
                    targetUser.getUserId(),
                    finalPoints,
                    finalReason,
                    userWallet.getBalancePoints()
            );

            // email cho leader/vice về việc trừ ví CLB
            membershipRepo.findByClub_ClubId(club.getClubId())
                    .stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER
                            || m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .forEach(m -> rewardService.sendClubWalletDeductionEmail(
                            m.getUser().getUserId(),
                            club.getName(),
                            finalPoints,
                            finalReason
                    ));

            return userWallet;
        }

        // ========= Admin / Staff thưởng trực tiếp ==========
        Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
        walletService.increase(userWallet, finalPoints);
        walletService.logTransactionFromSystem(
                userWallet,
                finalPoints,
                WalletTransactionTypeEnum.ADD,
                finalReason
        );

        rewardService.sendManualBonusEmail(
                targetUser.getUserId(),
                finalPoints,
                finalReason,
                userWallet.getBalancePoints()
        );

        return userWallet;
    }

    // ==============================================================
    // 🏛 2️⃣ NẠP ĐIỂM CHO CLUB (ADMIN / STAFF)
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

        // gửi email cho leader + vice-leader
        membershipRepo.findByClub_ClubId(clubId)
                .stream()
                .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                .forEach(m -> rewardService.sendClubTopUpEmail(
                        m.getUser().getUserId(),
                        club.getName(),
                        points,
                        (reason == null ? "Top-up by university staff" : reason)
                ));

        return clubWallet;
    }

    // ==============================================================
    // 🏆 3️⃣ THƯỞNG HÀNG LOẠT CHO NHIỀU CLB
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

            // email cho toàn bộ leader/vice
            membershipRepo.findByClub_ClubId(clubId)
                    .stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                            m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .forEach(m -> rewardService.sendClubTopUpEmail(
                            m.getUser().getUserId(),
                            club.getName(),
                            finalPoints,
                            req.getReason() + " (x" + clubMultiplier + ")"
                    ));

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
    // 👥 4️⃣ THƯỞNG HÀNG LOẠT CHO NHIỀU MEMBERS
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
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

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

                rewardService.sendManualBonusEmail(
                        u.getUserId(),
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
        //  B) LEADER / VICE thưởng thành viên CLB mình
        // ==============================================================
        if (isLeaderOrVice) {

            List<Membership> memberships = membershipRepo.findByUser_UserId(operator.getUserId());
            if (memberships.isEmpty()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not in any club.");
            }

            Club club = memberships.get(0).getClub();
            Wallet clubWallet = walletService.getOrCreateClubWallet(club);

            // lọc userIds thực sự là member (không có leader/vice)
            List<Long> realMemberIds =
                    membershipRepo.findByClub_ClubId(club.getClubId())
                            .stream()
                            .filter(m -> m.getClubRole() == ClubRoleEnum.MEMBER)
                            .filter(m -> m.getState() == MembershipStateEnum.APPROVED ||
                                    m.getState() == MembershipStateEnum.ACTIVE)
                            .map(m -> m.getUser().getUserId())
                            .filter(req.getTargetIds()::contains)
                            .toList();

            if (realMemberIds.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "No valid members to reward.");
            }

            double clubMultiplier = getClubMultiplier(club);
            long totalSpent = 0;
            int rewardCount = 0;

            for (Long memberId : realMemberIds) {

                User target = userRepo.findById(memberId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + memberId));

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

                rewardService.sendManualBonusEmail(
                        target.getUserId(),
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

            // biến final cho lambda
            final long totalSpentFinal = totalSpent;
            final int rewardCountFinal = rewardCount;

            // gửi email tổng kết cho leader + vice
            membershipRepo.findByClub_ClubId(club.getClubId())
                    .stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.LEADER ||
                            m.getClubRole() == ClubRoleEnum.VICE_LEADER)
                    .forEach(m -> rewardService.sendClubBatchDeductionSummaryEmail(
                            m.getUser().getUserId(),
                            club.getName(),
                            totalSpentFinal,
                            rewardCountFinal,
                            req.getReason()
                    ));
        }

        return responses;
    }

}
