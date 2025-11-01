package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.repository.WalletRepository;
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

    // ================================================================
    // 🎯 LẤY VÍ THEO USER ID (KHÔNG DÙNG NỮA)
    // ================================================================
    @Override
    public Wallet getWalletByUserId(Long userId) {
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "User no longer has a global wallet. Use membership wallet instead.");
    }

    // ================================================================
    // 🎁 THƯỞNG ĐIỂM CHO 1 THÀNH VIÊN
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

        // 🏫 Nếu người thưởng là CLB leader hoặc vice → trừ điểm từ ví CLB
        if (isLeader || isVice) {
            // Xác định CLB mà operator đang quản lý
            List<Membership> operatorMemberships = membershipRepo.findByUser_UserId(operator.getUserId());
            if (operatorMemberships.isEmpty()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of any club.");
            }

            // ✅ Nếu hệ thống có truyền clubId thì nên check theo clubId
            // Ở đây ta giả định leader chỉ được thưởng khi cùng CLB
            Long clubId = operatorMemberships.get(0).getClub().getClubId();

            Wallet clubWallet = walletService.getWalletByClubId(clubId);
            if (clubWallet.getBalancePoints() < points)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");
            walletService.decrease(clubWallet, points);
        }

        // 💰 Tạo hoặc lấy ví user được thưởng
        Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
        walletService.increase(userWallet, points);

        // 📜 Ghi log
        walletService.logClubToMemberReward(
                userWallet,
                points,
                reason == null ? "Manual reward" : reason
        );

        // ✉️ Gửi thông báo email thưởng
        long totalPoints = userWallet.getBalancePoints();
        rewardService.sendManualBonusEmail(
                targetUser.getUserId(),
                points,
                reason,
                totalPoints
        );

        // 🎖️ Kiểm tra mốc thưởng
        if (totalPoints >= 500 && totalPoints - points < 500)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 500);
        if (totalPoints >= 1000 && totalPoints - points < 1000)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 1000);
        if (totalPoints >= 2000 && totalPoints - points < 2000)
            rewardService.sendMilestoneEmail(targetUser.getUserId(), 2000);

        return userWallet;
    }


    // ================================================================
    // 🎯 THƯỞNG ĐIỂM CHO TOÀN BỘ THÀNH VIÊN TRONG CLB
    // ================================================================
    @Transactional
    @Override
    public int rewardPointsByClubId(User operator, Long clubId, long points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");

        //  Chỉ Admin / University Staff mới có quyền thưởng toàn CLB
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

            // Lấy ví user
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            // Cộng điểm cho ví user
            walletService.increase(userWallet, points);

            // Ghi transaction log
            walletService.logClubToMemberReward(
                    userWallet,
                    points,
                    reason == null ? "Mass reward" : reason
            );

            // Gửi email thông báo
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
    // 💰 UNI-STAFF NẠP ĐIỂM CHO CLB (CÓ NGƯỜI THỰC HIỆN)
    // ================================================================
    @Transactional
    @Override
    public Wallet topUpClubWallet(User operator, Long clubId, long points, String reason) {
        String role = operator.getRole().getRoleName();
        boolean isAdminOrStaff = role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("UNIVERSITY_STAFF");
        if (!isAdminOrStaff)
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only staff or admin can top up club wallets.");

        Club club = membershipRepo.findByClub_ClubId(clubId).stream()
                .findFirst()
                .map(Membership::getClub)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club not found."));

        Wallet clubWallet = walletService.getOrCreateClubWallet(club);

        // ✅ Gọi hàm mới để ghi tên người thao tác
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
    // 👥 THƯỞNG ĐIỂM HÀNG LOẠT CHO NHIỀU THÀNH VIÊN
    // ================================================================
    @Override
    @Transactional
    public List<WalletTransactionResponse> rewardMultipleMembers(WalletRewardBatchRequest req) {
        List<WalletTransactionResponse> responses = new ArrayList<>();

        for (Long membershipId : req.getTargetIds()) {
            Membership membership = membershipRepo.findById(membershipId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "Membership not found: " + membershipId));

            // 🎯 Lấy user & club từ membership
            User targetUser = membership.getUser();
            Club targetClub = membership.getClub();

            // ✅ Lấy ví CLB và ví User
            Wallet clubWallet = walletService.getOrCreateClubWallet(targetClub);
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            // ✅ Kiểm tra ví CLB có đủ điểm không
            if (clubWallet.getBalancePoints() < req.getPoints()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Insufficient club wallet balance for member: " + targetUser.getFullName());
            }

            // ✅ Chuyển điểm từ CLB → User
            walletService.transferPoints(clubWallet, userWallet, req.getPoints(), req.getReason());

            // ✅ Log giao dịch và build response
            responses.add(WalletTransactionResponse.builder()
                    .type("CLUB_TO_MEMBER")
                    .amount(req.getPoints())
                    .description(req.getReason())
                    .senderName(targetClub.getName())
                    .receiverName(targetUser.getFullName())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return responses;
    }

}
