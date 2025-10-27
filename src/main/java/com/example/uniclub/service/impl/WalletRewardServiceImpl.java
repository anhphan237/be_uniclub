package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletRewardServiceImpl implements WalletRewardService {

    private final WalletService walletService;
    private final MembershipRepository membershipRepo;
    private final RewardService rewardService;
    private final WalletRepository walletRepo;
    private final MajorPolicyService majorPolicyService;

    // ================================================================
    // 🎯 LẤY VÍ THEO MEMBERSHIP ID
    // ================================================================
    @Override
    public Wallet getWalletByUserId(Long userId) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "User no longer has a global wallet. Use membership wallet instead.");
    }

    // ================================================================
    // 🎁 THƯỞNG ĐIỂM CHO 1 THÀNH VIÊN (LEADER / VICE / STAFF / ADMIN)
    // ================================================================
    @Transactional
    @Override
    public Wallet rewardPointsByMembershipId(User operator, Long membershipId, int points, String reason) {
        // Kiểm tra quyền
        String role = operator.getRole().getRoleName();
        boolean isAdmin = role.equalsIgnoreCase("ADMIN");
        boolean isStaff = role.equalsIgnoreCase("UNIVERSITY_STAFF");
        boolean isLeader = role.equalsIgnoreCase("CLUB_LEADER");
        boolean isVice = role.equalsIgnoreCase("VICE_LEADER");

        if (!(isAdmin || isStaff || isLeader || isVice)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to reward points.");
        }

        // Lấy membership mục tiêu
        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found."));

        if (membership.getState() != MembershipStateEnum.APPROVED &&
                membership.getState() != MembershipStateEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership is not active or approved.");
        }

        Long targetClubId = membership.getClub().getClubId();

        // ✅ Nếu là Leader/Vice → chỉ được thưởng trong CLB của mình
        if (isLeader || isVice) {
            boolean isClubLeaderOrVice = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                    .anyMatch(m -> m.getClub().getClubId().equals(targetClubId) &&
                            (m.getClubRole().name().equalsIgnoreCase("LEADER") ||
                                    m.getClubRole().name().equalsIgnoreCase("VICE_LEADER")));
            if (!isClubLeaderOrVice)
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not the leader or vice leader of this club.");

            // Trừ điểm từ ví CLB
            Wallet clubWallet = walletService.getWalletByClubId(targetClubId);
            if (clubWallet.getBalancePoints() < points)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");
            walletService.decrease(clubWallet, points);
        }

        // 🎓 Multiplier theo ngành
        double majorMultiplier = 1.0;
        try {
            Long majorId = membership.getUser().getMajorId();
            if (majorId != null)
                majorMultiplier = majorPolicyService.getRewardMultiplierForMajor(majorId);
        } catch (Exception ignored) {}

        double clubMultiplier = 1.0;
        int finalPoints = (int) Math.round(points * (majorMultiplier * clubMultiplier));

        // ✅ Cộng điểm cho ví membership
        Wallet membershipWallet = walletService.getOrCreateMembershipWallet(membership);
        walletService.increase(membershipWallet, finalPoints);

        // 📩 Gửi mail + milestone
        int totalPoints = membershipWallet.getBalancePoints();
        rewardService.sendManualBonusEmail(membership.getUser().getUserId(), finalPoints, reason, totalPoints);
        if (totalPoints >= 500 && totalPoints - finalPoints < 500)
            rewardService.sendMilestoneEmail(membership.getUser().getUserId(), 500);
        if (totalPoints >= 1000 && totalPoints - finalPoints < 1000)
            rewardService.sendMilestoneEmail(membership.getUser().getUserId(), 1000);
        if (totalPoints >= 2000 && totalPoints - finalPoints < 2000)
            rewardService.sendMilestoneEmail(membership.getUser().getUserId(), 2000);

        return membershipWallet;
    }

    // ================================================================
    // 🎯 THƯỞNG ĐIỂM CHO TOÀN BỘ THÀNH VIÊN TRONG CLB
    // ================================================================
    @Transactional
    @Override
    public int rewardPointsByClubId(User operator, Long clubId, int points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN, "Only University Staff or admin can reward an entire club.");

        List<Membership> members = membershipRepo.findByClub_ClubId(clubId).stream()
                .filter(m -> m.getState() == MembershipStateEnum.APPROVED || m.getState() == MembershipStateEnum.ACTIVE)
                .toList();

        int count = 0;
        for (Membership m : members) {
            Wallet wallet = walletService.getOrCreateMembershipWallet(m);
            walletService.increase(wallet, points);
            rewardService.sendManualBonusEmail(m.getUser().getUserId(), points, reason, wallet.getBalancePoints());
            count++;
        }
        return count;
    }

    // ================================================================
    // 💰 UNI-STAFF NẠP ĐIỂM CHO CLB
    // ================================================================
    @Transactional
    @Override
    public Wallet topUpClubWallet(User operator, Long clubId, int points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");
        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN, "Only staff or admin can top up club wallets.");

        Club club = membershipRepo.findByClub_ClubId(clubId).stream()
                .findFirst()
                .map(Membership::getClub)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found."));

        Wallet clubWallet = walletService.getOrCreateClubWallet(club);
        walletService.addPoints(clubWallet, points, reason == null ? "Top-up by staff" : reason);
        return clubWallet;
    }
}
