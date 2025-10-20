package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
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

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletService.getWalletByUserId(userId);
    }

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

        if (membership.getState() != MembershipStateEnum.APPROVED && membership.getState() != MembershipStateEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership is not active or approved.");
        }

        Long targetUserId = membership.getUser().getUserId();
        Long targetClubId = membership.getClub().getClubId();

        // Leaders can only reward within their club
        if (isLeader) {
            boolean sameClub = membershipRepo.existsByUser_UserIdAndClub_ClubId(operator.getUserId(), targetClubId);
            if (!sameClub) throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of this club.");
        }

        Wallet wallet = walletService.getWalletByUserId(targetUserId);
        walletService.increase(wallet, points);
        int totalPoints = wallet.getBalancePoints();

        rewardService.sendManualBonusEmail(targetUserId, points, reason, totalPoints);

        if (totalPoints >= 500 && totalPoints - points < 500) rewardService.sendMilestoneEmail(targetUserId, 500);
        if (totalPoints >= 1000 && totalPoints - points < 1000) rewardService.sendMilestoneEmail(targetUserId, 1000);
        if (totalPoints >= 2000 && totalPoints - points < 2000) rewardService.sendMilestoneEmail(targetUserId, 2000);

        return wallet;
    }

    // ✅ Reward all approved members in a club (Staff/Admin only)
    @Transactional
    @Override
    public int rewardPointsByClubId(User operator, Long clubId, int points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN, "Only staff or admin can reward an entire club.");

        List<Membership> members = membershipRepo.findByClub_ClubIdAndState(clubId, MembershipStateEnum.APPROVED);
        int count = 0;

        for (Membership m : members) {
            Wallet wallet = walletService.getWalletByUserId(m.getUser().getUserId());
            walletService.increase(wallet, points);
            rewardService.sendManualBonusEmail(m.getUser().getUserId(), points, reason, wallet.getBalancePoints());
            count++;
        }

        return count;
    }
}
