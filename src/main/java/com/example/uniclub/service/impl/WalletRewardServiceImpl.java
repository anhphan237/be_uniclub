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
    // 🎯 LẤY VÍ THEO USER ID (KHÔNG DÙNG NỮA)
    // ================================================================
    @Override
    public Wallet getWalletByUserId(Long userId) {
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "User no longer has a global wallet. Use membership wallet instead.");
    }

    // ================================================================
    // 🎁 THƯỞNG ĐIỂM CHO 1 THÀNH VIÊN (có multiplier)
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

        // 🎯 Tìm user được thưởng
        User targetUser = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        // 🧮 Tính multiplier cơ bản
        int attendedEvents = membershipRepo.countByUser_UserIdAndState(userId, MembershipStateEnum.ACTIVE);
        double memberMultiplier = getMemberMultiplier(attendedEvents);
        double clubMultiplier = 1.0; // sẽ tính riêng nếu có eventRepo

        double totalPoints = points * memberMultiplier * clubMultiplier;

        // 🏫 Nếu là leader/vice -> trừ điểm từ ví CLB
        if (isLeader || isVice) {
            List<Membership> operatorMemberships = membershipRepo.findByUser_UserId(operator.getUserId());
            if (operatorMemberships.isEmpty()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of any club.");
            }
            Long clubId = operatorMemberships.get(0).getClub().getClubId();
            Wallet clubWallet = walletService.getWalletByClubId(clubId);
            if (clubWallet.getBalancePoints() < totalPoints)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");
            walletService.decrease(clubWallet, (long) totalPoints);
        }

        // 💰 Tạo hoặc lấy ví user
        Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
        walletService.increase(userWallet, (long) totalPoints);

        // 📜 Ghi log giao dịch
        walletService.logClubToMemberReward(
                userWallet,
                (long) totalPoints,
                reason == null ? "Manual reward (with multiplier)" : reason
        );

        // ✉️ Gửi email
        long totalBalance = userWallet.getBalancePoints();
        rewardService.sendManualBonusEmail(targetUser.getUserId(), (long) totalPoints, reason, totalBalance);

        // 🎖️ Kiểm tra mốc thưởng
        if (totalBalance >= 500 && totalBalance - totalPoints < 500)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 500);
        if (totalBalance >= 1000 && totalBalance - totalPoints < 1000)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 1000);
        if (totalBalance >= 2000 && totalBalance - totalPoints < 2000)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 2000);

        return userWallet;
    }

    // ================================================================
    // 🎯 THƯỞNG ĐIỂM CHO TOÀN BỘ THÀNH VIÊN TRONG CLB (đúng interface)
    // ================================================================
    @Transactional
    @Override
    public int rewardPointsByClubId(User operator, Long clubId, long points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only University Staff or Admin can reward an entire club.");

        List<Membership> activeMembers = membershipRepo.findByClub_ClubId(clubId).stream()
                .filter(m -> m.getState() == MembershipStateEnum.APPROVED ||
                        m.getState() == MembershipStateEnum.ACTIVE)
                .toList();

        if (activeMembers.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "No active or approved members found in this club.");

        Wallet clubWallet = walletService.getWalletByClubId(clubId);
        long totalPointsNeeded = points * activeMembers.size();

        if (clubWallet.getBalancePoints() < totalPointsNeeded)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance for mass reward.");

        walletService.decrease(clubWallet, totalPointsNeeded);

        int count = 0;
        for (Membership m : activeMembers) {
            User targetUser = m.getUser();
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            walletService.increase(userWallet, points);
            walletService.logClubToMemberReward(
                    userWallet,
                    points,
                    reason == null ? "Mass reward" : reason
            );

            rewardService.sendManualBonusEmail(
                    targetUser.getUserId(),
                    points,
                    reason,
                    userWallet.getBalancePoints()
            );

            count++;
        }

        return count;
    }

    // ================================================================
    // 💰 NẠP ĐIỂM CHO CLB
    // ================================================================
    @Transactional
    @Override
    public Wallet topUpClubWallet(User operator, Long clubId, long points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");
        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only staff or admin can top up club wallets.");

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found."));

        Wallet clubWallet = walletService.getOrCreateClubWallet(club);

        walletService.topupPointsFromUniversityWithOperator(
                clubWallet.getWalletId(),
                points,
                reason == null ? "Top-up by staff" : reason,
                operator.getFullName()
        );

        return clubWallet;
    }

    // ================================================================
    // 🏦 THƯỞNG ĐIỂM HÀNG LOẠT CHO NHIỀU CLB
    // ================================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleClubs(WalletRewardBatchRequest req) {
        List<WalletTransactionResponse> responses = new ArrayList<>();

        for (Long clubId : req.getTargetIds()) {
            Wallet clubWallet = walletService.getOrCreateClubWallet(
                    clubRepo.findById(clubId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found: " + clubId))
            );

            walletService.topupPointsFromUniversity(
                    clubWallet,
                    req.getPoints(),
                    req.getReason()
            );

            responses.add(WalletTransactionResponse.builder()
                    .type("UNI_TO_CLUB")
                    .amount(req.getPoints())
                    .description(req.getReason())
                    .receiverName(clubWallet.getClub().getName())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return responses;
    }

    // ================================================================
    // 👥 THƯỞNG ĐIỂM HÀNG LOẠT CHO NHIỀU THÀNH VIÊN (có multiplier)
    // ================================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleMembers(WalletRewardBatchRequest req) {
        List<WalletTransactionResponse> responses = new ArrayList<>();

        for (Long membershipId : req.getTargetIds()) {
            Membership membership = membershipRepo.findById(membershipId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Membership not found: " + membershipId));

            User targetUser = membership.getUser();
            Club targetClub = membership.getClub();

            Wallet clubWallet = walletService.getOrCreateClubWallet(targetClub);
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            int attendedEvents = membershipRepo.countByUser_UserIdAndState(targetUser.getUserId(), MembershipStateEnum.ACTIVE);
            double memberMultiplier = getMemberMultiplier(attendedEvents);
            double clubMultiplier = getClubMultiplier(0); // thay bằng count thực tế nếu có

            double totalPoints = req.getPoints() * memberMultiplier * clubMultiplier;

            if (clubWallet.getBalancePoints() < totalPoints) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Insufficient club wallet balance for member: " + targetUser.getFullName());
            }

            walletService.transferPoints(clubWallet, userWallet, (long) totalPoints, req.getReason());

            responses.add(WalletTransactionResponse.builder()
                    .type("CLUB_TO_MEMBER")
                    .amount((long) totalPoints)
                    .description(req.getReason())
                    .senderName(targetClub.getName())
                    .receiverName(targetUser.getFullName())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return responses;
    }
}
