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

        if (!(isAdmin || isStaff || isLeader)) {
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

        // ✅ Nếu là Leader, phải thuộc cùng CLB và ví CLB bị trừ điểm
        if (isLeader) {
            boolean isClubLeader = membershipRepo.findByUser_UserId(operator.getUserId()).stream()
                    .anyMatch(m -> m.getClub().getClubId().equals(targetClubId)
                            && m.getClubRole().name().equalsIgnoreCase("LEADER"));
            if (!isClubLeader) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not the leader of this club.");
            }

            // Trừ điểm từ ví CLB
            Wallet clubWallet = walletService.getWalletByClubId(targetClubId);
            if (clubWallet.getBalancePoints() < points) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");
            }
            walletService.decrease(clubWallet, points);
        }

        // ✅ Cộng điểm cho user
        Wallet userWallet = walletRepository.findByUser_UserId(targetUserId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(membership.getUser());
                    newWallet.setOwnerType(WalletOwnerTypeEnum.USER);
                    newWallet.setBalancePoints(0);
                    return walletRepository.save(newWallet);
                });

        walletService.increase(userWallet, points);
        int totalPoints = userWallet.getBalancePoints();

        // ✅ Gửi mail và milestone (nếu có)
        rewardService.sendManualBonusEmail(targetUserId, points, reason, totalPoints);
        if (totalPoints >= 500 && totalPoints - points < 500) rewardService.sendMilestoneEmail(targetUserId, 500);
        if (totalPoints >= 1000 && totalPoints - points < 1000) rewardService.sendMilestoneEmail(targetUserId, 1000);
        if (totalPoints >= 2000 && totalPoints - points < 2000) rewardService.sendMilestoneEmail(targetUserId, 2000);

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
