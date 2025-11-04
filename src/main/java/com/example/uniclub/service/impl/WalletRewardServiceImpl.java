package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.*;
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
    private final MultiplierPolicyService multiplierPolicyService;

    // ================================================================
    // ⚙️ Helper: Lấy multiplier theo policy
    // ================================================================
    private double getMemberMultiplier(int attendedEvents) {
        List<MultiplierPolicy> policies = multiplierPolicyService.getPolicies(PolicyTargetTypeEnum.MEMBER);
        for (MultiplierPolicy p : policies) {
            if (attendedEvents >= p.getMinEvents() && p.isActive()) {
                return p.getMultiplier();
            }
        }
        return 1.0;
    }

    private double getClubMultiplier(int hostedEvents) {
        List<MultiplierPolicy> policies = multiplierPolicyService.getPolicies(PolicyTargetTypeEnum.CLUB);
        for (MultiplierPolicy p : policies) {
            if (hostedEvents >= p.getMinEvents() && p.isActive()) {
                return p.getMultiplier();
            }
        }
        return 1.0;
    }

    // ================================================================
    // 🎁 THƯỞNG ĐIỂM CHO 1 USER (có multiplier)
    // ================================================================
    @Transactional
    @Override
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

        int attendedEvents = membershipRepo.countByUser_UserIdAndState(userId, MembershipStateEnum.ACTIVE);
        double memberMultiplier = getMemberMultiplier(attendedEvents);
        double totalPoints = points * memberMultiplier;
        String finalReason = reason == null ? "Manual reward (with multiplier)" : reason;

        if (isLeader || isVice) {
            List<Membership> operatorMemberships = membershipRepo.findByUser_UserId(operator.getUserId());
            if (operatorMemberships.isEmpty()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of any club.");
            }

            Long clubId = operatorMemberships.get(0).getClub().getClubId();
            Wallet clubWallet = walletService.getWalletByClubId(clubId);
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            if (clubWallet.getBalancePoints() < totalPoints)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

            walletService.transferPointsWithType(
                    clubWallet,
                    userWallet,
                    (long) totalPoints,
                    finalReason,
                    WalletTransactionTypeEnum.CLUB_TO_MEMBER
            );

            rewardService.sendManualBonusEmail(targetUser.getUserId(), (long) totalPoints, finalReason, userWallet.getBalancePoints());
            checkMilestones(targetUser, userWallet.getBalancePoints(), totalPoints);
            return userWallet;
        }

        Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
        walletService.increase(userWallet, (long) totalPoints);
        walletService.logTransactionFromSystem(
                userWallet,
                (long) totalPoints,
                WalletTransactionTypeEnum.ADD,
                finalReason
        );

        rewardService.sendManualBonusEmail(targetUser.getUserId(), (long) totalPoints, finalReason, userWallet.getBalancePoints());
        checkMilestones(targetUser, userWallet.getBalancePoints(), totalPoints);

        return userWallet;
    }

    private void checkMilestones(User targetUser, long totalBalance, double totalPoints) {
        if (totalBalance >= 500 && totalBalance - totalPoints < 500)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 500);
        if (totalBalance >= 1000 && totalBalance - totalPoints < 1000)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 1000);
        if (totalBalance >= 2000 && totalBalance - totalPoints < 2000)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 2000);
    }

    // ================================================================
    // 💰 NẠP ĐIỂM CHO CLB
    // ================================================================
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
                (reason == null ? "Top-up by staff" : reason),
                operator.getFullName()
        );

        return clubWallet;
    }

    // ================================================================
    // 🏦 THƯỞNG HÀNG LOẠT CHO NHIỀU CLB
    // ================================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleClubs(WalletRewardBatchRequest req) {
        List<WalletTransactionResponse> responses = new ArrayList<>();

        for (Long clubId : req.getTargetIds()) {
            Club club = clubRepo.findById(clubId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found: " + clubId));

            Wallet clubWallet = walletService.getOrCreateClubWallet(club);
            WalletTransaction tx = walletService.topupPointsFromUniversity(
                    clubWallet,
                    req.getPoints(),
                    req.getReason()
            );

            responses.add(WalletTransactionResponse.builder()
                    .id(tx.getId())
                    .type(tx.getType().name())
                    .amount(tx.getAmount())
                    .signedAmount("+" + tx.getAmount())  // ✅ thêm hiển thị dấu +
                    .description(tx.getDescription())
                    .senderName(tx.getSenderName())
                    .receiverName(club.getName())
                    .createdAt(tx.getCreatedAt())
                    .build());
        }

        return responses;
    }

    // ================================================================
    // 👥 THƯỞNG HÀNG LOẠT CHO NHIỀU THÀNH VIÊN (chỉ MEMBER thật)
    // ================================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleMembers(User operator, WalletRewardBatchRequest req) {
        List<WalletTransactionResponse> responses = new ArrayList<>();

        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");
        boolean isLeaderOrVice = role.equalsIgnoreCase("CLUB_LEADER") || role.equalsIgnoreCase("VICE_LEADER");

        // 🎓 Admin / Staff → thưởng trực tiếp
        if (isAdminOrStaff) {
            for (Long userId : req.getTargetIds()) {
                User targetUser = userRepo.findById(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

                Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
                walletService.increase(userWallet, req.getPoints());
                walletService.logTransactionFromSystem(
                        userWallet,
                        req.getPoints(),
                        WalletTransactionTypeEnum.ADD,
                        req.getReason()
                );

                rewardService.sendManualBonusEmail(
                        targetUser.getUserId(),
                        req.getPoints(),
                        req.getReason(),
                        userWallet.getBalancePoints()
                );

                responses.add(WalletTransactionResponse.builder()
                        .type("UNI_TO_MEMBER")
                        .amount(req.getPoints())
                        .signedAmount("+" + req.getPoints()) // ✅ thêm dấu +
                        .description(req.getReason())
                        .senderName("University System")
                        .receiverName(targetUser.getFullName())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            return responses;
        }

        // 🏫 Leader / Vice → thưởng cho member (loại bỏ leader/vice)
        if (isLeaderOrVice) {
            Club club = clubRepo.findByLeader_UserId(operator.getUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a leader of any club."));
            Wallet clubWallet = walletService.getOrCreateClubWallet(club);

            List<Long> memberIds = membershipRepo.findByClub_ClubId(club.getClubId()).stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.MEMBER)
                    .filter(m -> m.getState() == MembershipStateEnum.APPROVED || m.getState() == MembershipStateEnum.ACTIVE)
                    .map(m -> m.getUser().getUserId())
                    .filter(req.getTargetIds()::contains)
                    .toList();

            if (memberIds.isEmpty())
                throw new ApiException(HttpStatus.BAD_REQUEST, "No valid members to reward.");

            for (Long userId : memberIds) {
                User targetUser = userRepo.findById(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

                Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

                if (clubWallet.getBalancePoints() < req.getPoints())
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

                walletService.transferPointsWithType(
                        clubWallet,
                        userWallet,
                        req.getPoints(),
                        req.getReason(),
                        WalletTransactionTypeEnum.CLUB_TO_MEMBER
                );

                rewardService.sendManualBonusEmail(
                        targetUser.getUserId(),
                        req.getPoints(),
                        req.getReason(),
                        userWallet.getBalancePoints()
                );

                responses.add(WalletTransactionResponse.builder()
                        .type("CLUB_TO_MEMBER")
                        .amount(req.getPoints())
                        .signedAmount("+" + req.getPoints()) // ✅ thêm dấu +
                        .description(req.getReason())
                        .senderName(club.getName())
                        .receiverName(targetUser.getFullName())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        return responses;
    }
}
