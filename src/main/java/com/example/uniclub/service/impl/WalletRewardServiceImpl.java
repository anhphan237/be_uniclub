package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.service.MajorPolicyService;
import com.example.uniclub.service.RewardService;
import com.example.uniclub.service.WalletRewardService;
import com.example.uniclub.service.WalletService;
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
    private final UserRepository userRepo;
    private final RewardService rewardService;
    private final WalletRepository walletRepository;
    private final MajorPolicyService majorPolicyService;

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletService.getWalletByUserId(userId);
    }

    // ✅ Reward 1 member
    @Transactional
    @Override
    public Wallet rewardPointsByMembershipId(User operator, Long membershipId, int points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdmin = role.equalsIgnoreCase("ADMIN");
        boolean isStaff = role.equalsIgnoreCase("UNIVERSITY_STAFF");
        boolean isLeader = role.equalsIgnoreCase("CLUB_LEADER");
        boolean isVice = role.equalsIgnoreCase("VICE_LEADER"); // ✅ Vice có quyền tương đương Leader

        if (!(isAdmin || isStaff || isLeader || isVice)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to reward points.");
        }

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found."));

        if (membership.getState() != MembershipStateEnum.APPROVED &&
                membership.getState() != MembershipStateEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership is not active or approved.");
        }

        Long targetUserId = membership.getUser().getUserId();
        Long targetClubId = membership.getClub().getClubId();

        // ✅ Nếu là Leader hoặc Vice, phải thuộc cùng CLB và ví CLB bị trừ điểm
        if (isLeader || isVice) {
            boolean isClubLeaderOrVice = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                    .anyMatch(m -> m.getClub().getClubId().equals(targetClubId) &&
                            (m.getClubRole().name().equalsIgnoreCase("LEADER") ||
                                    m.getClubRole().name().equalsIgnoreCase("VICE_LEADER")));
            if (!isClubLeaderOrVice) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not the leader or vice leader of this club.");
            }

            // Trừ điểm từ ví CLB
            Wallet clubWallet = walletService.getWalletByClubId(targetClubId);
            if (clubWallet.getBalancePoints() < points) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");
            }
            walletService.decrease(clubWallet, points);
        }

        // 🎓 Tính multiplier theo Major (dựa trên MajorPolicy)
        double majorMultiplier = 1.0;
        try {
            Long majorId = membership.getUser().getMajorId();
            if (majorId != null) {
                majorMultiplier = majorPolicyService.getRewardMultiplierForMajor(majorId);
            }

        } catch (Exception e) {
            // Không có policy nào cho major này => giữ nguyên 1.0
            majorMultiplier = 1.0;
        }

        // 🏫 Club multiplier (có thể mở rộng sau này)
        double clubMultiplier = 1.0;

        // 💰 Tổng multiplier
        double totalMultiplier = majorMultiplier * clubMultiplier;
        int finalPoints = (int) Math.round(points * totalMultiplier);

        // ✅ Cộng điểm cho user
        Wallet userWallet = walletRepository.findByUser_UserId(targetUserId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(membership.getUser());
                    newWallet.setOwnerType(WalletOwnerTypeEnum.USER);
                    newWallet.setBalancePoints(0);
                    return walletRepository.save(newWallet);
                });

        walletService.increase(userWallet, finalPoints);
        int totalPoints = userWallet.getBalancePoints();

        // ✅ Gửi mail + milestone (tính theo tổng điểm)
        rewardService.sendManualBonusEmail(targetUserId, finalPoints, reason, totalPoints);
        if (totalPoints >= 500 && totalPoints - finalPoints < 500)
            rewardService.sendMilestoneEmail(targetUserId, 500);
        if (totalPoints >= 1000 && totalPoints - finalPoints < 1000)
            rewardService.sendMilestoneEmail(targetUserId, 1000);
        if (totalPoints >= 2000 && totalPoints - finalPoints < 2000)
            rewardService.sendMilestoneEmail(targetUserId, 2000);

        return userWallet;
    }


    // ✅ Reward tất cả members của CLB (Admin/Staff)
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
            Long userId = m.getUser().getUserId();

            Wallet wallet = walletRepository.findByUser_UserId(userId)
                    .orElseGet(() -> {
                        Wallet newWallet = new Wallet();
                        newWallet.setUser(m.getUser());
                        newWallet.setOwnerType(WalletOwnerTypeEnum.USER);
                        newWallet.setBalancePoints(0);
                        return walletRepository.save(newWallet);
                    });

            walletService.increase(wallet, points);
            rewardService.sendManualBonusEmail(userId, points, reason, wallet.getBalancePoints());
            count++;
        }

        return count;
    }

    // ✅ UniStaff/Admin nạp điểm vào ví CLB
    @Transactional
    @Override
    public Wallet topUpClubWallet(User operator, Long clubId, int points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");
        if (!isAdminOrStaff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only staff or admin can top up club wallets.");
        }

        Wallet wallet = walletRepository.findByClub_ClubId(clubId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setClub(membershipRepo.findByClub_ClubId(clubId).get(0).getClub());
                    newWallet.setOwnerType(WalletOwnerTypeEnum.CLUB);
                    newWallet.setBalancePoints(0);
                    return walletRepository.save(newWallet);
                });

        walletService.increase(wallet, points);
        return wallet;
    }
}
