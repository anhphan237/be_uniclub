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

@Service
@RequiredArgsConstructor
public class WalletRewardServiceImpl implements WalletRewardService {

    private final WalletService walletService;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletService.getWalletByUserId(userId);
    }

    @Transactional
    @Override
    public Wallet rewardPointsByMembershipId(User operator, Long membershipId, int points, String reason) {
        // 1️⃣ Kiểm tra quyền hệ thống
        String sysRole = operator.getRole().getRoleName();
        boolean isAdminOrStaff = "ADMIN".equalsIgnoreCase(sysRole) || "UNIVERSITY_STAFF".equalsIgnoreCase(sysRole);
        boolean isClubLeader = "CLUB_LEADER".equalsIgnoreCase(sysRole);

        if (!(isAdminOrStaff || isClubLeader)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền phát điểm");
        }

        // 2️⃣ Lấy membership người nhận
        Membership memberMs = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found: " + membershipId));

        if (memberMs.getState() != MembershipStateEnum.APPROVED && memberMs.getState() != MembershipStateEnum.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership chưa được duyệt hoặc đã hết hiệu lực");
        }

        Long receiverUserId = memberMs.getUser().getUserId();
        Long receiverClubId = memberMs.getClub().getClubId();

        // 3️⃣ Nếu là CLUB_LEADER, phải cùng CLB và có role LEADER/VICE_LEADER
        if (isClubLeader) {
            boolean sameClub = membershipRepository.existsByUser_UserIdAndClub_ClubId(operator.getUserId(), receiverClubId);
            if (!sameClub) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không thuộc CLB của thành viên này");
            }

            boolean isLeaderOrVice = membershipRepository.findByUser_UserId(operator.getUserId()).stream()
                    .filter(ms -> ms.getClub().getClubId().equals(receiverClubId))
                    .anyMatch(ms -> ms.getClubRole() == ClubRoleEnum.LEADER || ms.getClubRole() == ClubRoleEnum.VICE_LEADER);

            if (!isLeaderOrVice) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Bạn cần là LEADER/VICE_LEADER trong CLB này");
            }
        }

        // 4️⃣ Cộng điểm và gửi mail
        Wallet receiverWallet = walletService.getWalletByUserId(receiverUserId);
        walletService.increase(receiverWallet, points);
        int totalPoints = receiverWallet.getBalancePoints();

        rewardService.sendManualBonusEmail(receiverUserId, points, reason, totalPoints);

        // 5️⃣ Gửi mail milestone
        if (totalPoints >= 500 && (totalPoints - points) < 500) rewardService.sendMilestoneEmail(receiverUserId, 500);
        if (totalPoints >= 1000 && (totalPoints - points) < 1000) rewardService.sendMilestoneEmail(receiverUserId, 1000);
        if (totalPoints >= 2000 && (totalPoints - points) < 2000) rewardService.sendMilestoneEmail(receiverUserId, 2000);

        return receiverWallet;
    }
}
