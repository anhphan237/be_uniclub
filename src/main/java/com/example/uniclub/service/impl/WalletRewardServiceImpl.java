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
import java.util.Comparator;
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
    private final ClubAttendanceRecordRepository clubAttendanceRecordRepo;
    private final EventRepository eventRepo;

    // ================================================================
    // ⚙️ Helper: Lấy multiplier theo policy
    // ================================================================
    private double getMemberMultiplier(User member) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeMonthsAgo = now.minusMonths(3);

        // 🧮 1️⃣ Lấy tất cả attendance trong 3 tháng gần nhất
        List<ClubAttendanceRecord> attendanceList = clubAttendanceRecordRepo
                .findByMembership_User_UserIdAndSession_CreatedAtBetween(
                        member.getUserId(),
                        threeMonthsAgo,
                        now
                );

        int totalEvents = attendanceList.size();
        if (totalEvents == 0) return 1.0;

        long attended = attendanceList.stream()
                .filter(a -> a.getStatus() == AttendanceStatusEnum.PRESENT || a.getStatus() == AttendanceStatusEnum.LATE)
                .count();

        double attendanceRate = (double) attended / totalEvents * 100; // % chuyên cần

        // 🧩 2️⃣ Lấy danh sách chính sách multiplier cho MEMBER từ DB (sắp xếp giảm dần)
        List<MultiplierPolicy> policies = multiplierPolicyService
                .getActiveEntityByTargetType(PolicyTargetTypeEnum.MEMBER);


        // 🔍 3️⃣ Chọn policy phù hợp nhất (ngưỡng attendanceRate ≥ minEventsForClub)
        MultiplierPolicy matchedPolicy = policies.stream()
                .filter(p -> attendanceRate >= (p.getMinEventsForClub() != null ? p.getMinEventsForClub() : 0))
                .findFirst()
                .orElse(null);

        // ⚙️ 4️⃣ Kiểm tra xem member có duy trì ≥80% attendance 3 tháng liên tục (ELITE)
        boolean sustainedHighAttendance = true;
        for (int i = 0; i < 3; i++) {
            LocalDateTime start = now.minusMonths(i + 1);
            LocalDateTime end = now.minusMonths(i);

            int monthTotal = clubAttendanceRecordRepo.countByMembership_User_UserIdAndSession_CreatedAtBetween(
                    member.getUserId(), start, end);
            int monthPresent = clubAttendanceRecordRepo.countByMembership_User_UserIdAndStatusInAndSession_CreatedAtBetween(
                    member.getUserId(),
                    List.of(AttendanceStatusEnum.PRESENT, AttendanceStatusEnum.LATE),
                    start, end);

            double monthRate = monthTotal == 0 ? 0 : (double) monthPresent / monthTotal;
            if (monthRate < 0.8) {
                sustainedHighAttendance = false;
                break;
            }
        }

        // 🎯 5️⃣ Xác định cấp độ tương ứng
        MemberLevelEnum level = MemberLevelEnum.BASIC;
        if (matchedPolicy != null) {
            try {
                level = MemberLevelEnum.valueOf(matchedPolicy.getLevelOrStatus());
            } catch (IllegalArgumentException ignored) {
                level = MemberLevelEnum.BASIC;
            }
        }

        // Nếu chuyên cần 3 tháng liên tiếp ≥80% → ép thành ELITE (nếu có chính sách)
        if (sustainedHighAttendance) {
            MultiplierPolicy elitePolicy = policies.stream()
                    .filter(p -> "ELITE".equalsIgnoreCase(p.getLevelOrStatus()))
                    .findFirst()
                    .orElse(null);
            if (elitePolicy != null) {
                matchedPolicy = elitePolicy;
                level = MemberLevelEnum.ELITE;
            }
        }

        // 💰 6️⃣ Trả multiplier tương ứng
        double multiplier = matchedPolicy != null ? matchedPolicy.getMultiplier() : 1.0;

        // (tuỳ chọn) Cập nhật lại memberLevel vào Membership
        final MemberLevelEnum finalLevel = level;
        final double finalMultiplier = multiplier;
        membershipRepo.findByUser_UserId(member.getUserId())
                .stream()
                .findFirst()
                .ifPresent(m -> {
                    m.setMemberLevel(finalLevel);
                    m.setMemberMultiplier(finalMultiplier);
                    membershipRepo.save(m);
                });

        return multiplier;
    }




    private double getClubMultiplier(Club club) {
        // 📅 Đếm số sự kiện đã hoàn thành của CLB
        long completedEvents = (long) eventRepo.countByHostClub_ClubIdAndStatus(
                club.getClubId(),
                EventStatusEnum.COMPLETED
        );


        // 🧠 Xác định trạng thái hoạt động dựa trên số sự kiện
        ClubActivityStatusEnum status;
        if (completedEvents < 2) status = ClubActivityStatusEnum.INACTIVE;
        else if (completedEvents < 5) status = ClubActivityStatusEnum.ACTIVE;
        else status = ClubActivityStatusEnum.EXCELLENT;

        // 💰 Tìm multiplier tương ứng trong bảng policy
        return multiplierPolicyService
                .findByTargetTypeAndLevelOrStatus(PolicyTargetTypeEnum.CLUB, status.name())
                .map(MultiplierPolicy::getMultiplier)
                .orElse(1.0);
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

        double memberMultiplier = getMemberMultiplier(targetUser);
        long totalPoints = Math.round(points * memberMultiplier);
        String finalReason = (reason == null ? "Manual reward (with multiplier)" : reason);

        // ======================================================
        // 🎓 Leader / Vice: dùng ví CLB để thưởng member
        // ======================================================
        if (isLeader || isVice) {
            List<Membership> operatorMemberships = membershipRepo.findByUser_UserId(operator.getUserId());
            if (operatorMemberships.isEmpty()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of any club.");
            }

            Club club = operatorMemberships.get(0).getClub();
            Long clubId = club.getClubId();

            Wallet clubWallet = walletService.getWalletByClubId(clubId);
            Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);

            if (clubWallet.getBalancePoints() < totalPoints)
                throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

            walletService.transferPointsWithType(
                    clubWallet,
                    userWallet,
                    totalPoints,
                    finalReason,
                    WalletTransactionTypeEnum.CLUB_TO_MEMBER
            );

            // 📧 Email cho member được thưởng
            rewardService.sendManualBonusEmail(
                    targetUser.getUserId(),
                    totalPoints,
                    finalReason,
                    userWallet.getBalancePoints()
            );
            checkMilestones(targetUser, userWallet.getBalancePoints(), totalPoints);

            // 📧 NEW: Email cho LEADER + VICE_LEADER khi ví CLB bị trừ
            List<Membership> leaders = membershipRepo.findByClub_ClubId(clubId);
            for (Membership m : leaders) {
                if (m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER) {

                    User u = m.getUser();
                    rewardService.sendClubWalletDeductionEmail(
                            u.getUserId(),
                            club.getName(),
                            totalPoints,
                            finalReason
                    );
                }
            }

            return userWallet;
        }

        // ======================================================
        // 🏛 Admin / Staff: thưởng trực tiếp từ hệ thống
        // ======================================================
        Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
        walletService.increase(userWallet, totalPoints);
        walletService.logTransactionFromSystem(
                userWallet,
                totalPoints,
                WalletTransactionTypeEnum.ADD,
                finalReason
        );

        rewardService.sendManualBonusEmail(
                targetUser.getUserId(),
                totalPoints,
                finalReason,
                userWallet.getBalancePoints()
        );
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

        // ================================================
        // 📧 SEND EMAIL TO LEADER + VICE-LEADER
        // ================================================
        List<Membership> leaders = membershipRepo.findByClub_ClubId(clubId);
        for (Membership m : leaders) {
            if (m.getClubRole() == ClubRoleEnum.LEADER ||
                    m.getClubRole() == ClubRoleEnum.VICE_LEADER) {

                User target = m.getUser();

                rewardService.sendClubTopUpEmail(
                        target.getUserId(),
                        club.getName(),
                        points,
                        (reason == null ? "Top-up by staff" : reason)
                );
            }
        }

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

            // 🧮 Tính multiplier của CLB dựa trên số sự kiện đã hoàn thành
            double clubMultiplier = getClubMultiplier(club);

            // 💰 Tính số điểm cuối cùng sau khi nhân hệ số
            long finalPoints = Math.round(req.getPoints() * clubMultiplier);

            Wallet clubWallet = walletService.getOrCreateClubWallet(club);
            WalletTransaction tx = walletService.topupPointsFromUniversity(
                    clubWallet,
                    finalPoints,
                    req.getReason() + " (x" + clubMultiplier + ")"
            );

            // 📧 NEW: Gửi email cho LEADER + VICE_LEADER của CLB
            List<Membership> leaders = membershipRepo.findByClub_ClubId(clubId);
            for (Membership m : leaders) {
                if (m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER) {

                    User u = m.getUser();
                    rewardService.sendClubTopUpEmail(
                            u.getUserId(),
                            club.getName(),
                            finalPoints,
                            req.getReason() + " (x" + clubMultiplier + ")"
                    );
                }
            }

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

        // ================================================================
        // 🎓 1️⃣ Admin / Staff → thưởng trực tiếp (dựa theo memberMultiplier)
        // ================================================================
        if (isAdminOrStaff) {
            for (Long userId : req.getTargetIds()) {
                User targetUser = userRepo.findById(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

                double memberMultiplier = getMemberMultiplier(targetUser);
                long totalPoints = Math.round(req.getPoints() * memberMultiplier);

                Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
                walletService.increase(userWallet, totalPoints);
                walletService.logTransactionFromSystem(
                        userWallet,
                        totalPoints,
                        WalletTransactionTypeEnum.ADD,
                        req.getReason() + String.format(" (x%.2f)", memberMultiplier)
                );

                rewardService.sendManualBonusEmail(
                        targetUser.getUserId(),
                        totalPoints,
                        req.getReason(),
                        userWallet.getBalancePoints()
                );

                responses.add(WalletTransactionResponse.builder()
                        .type("UNI_TO_MEMBER")
                        .amount(totalPoints)
                        .signedAmount("+" + totalPoints)
                        .description(req.getReason() + String.format(" (x%.2f)", memberMultiplier))
                        .senderName("University System")
                        .receiverName(targetUser.getFullName())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            return responses;
        }

        // ================================================================
        // 🏫 2️⃣ Leader / Vice → thưởng cho member CLB (loại bỏ leader/vice)
        // ================================================================
        if (isLeaderOrVice) {
            Club club = clubRepo.findByLeader_UserId(operator.getUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a leader of any club."));
            Wallet clubWallet = walletService.getOrCreateClubWallet(club);

            // 🔍 Lấy danh sách member thật sự
            List<Long> memberIds = membershipRepo.findByClub_ClubId(club.getClubId()).stream()
                    .filter(m -> m.getClubRole() == ClubRoleEnum.MEMBER)
                    .filter(m -> m.getState() == MembershipStateEnum.APPROVED || m.getState() == MembershipStateEnum.ACTIVE)
                    .map(m -> m.getUser().getUserId())
                    .filter(req.getTargetIds()::contains)
                    .toList();

            if (memberIds.isEmpty())
                throw new ApiException(HttpStatus.BAD_REQUEST, "No valid members to reward.");

            double clubMultiplier = getClubMultiplier(club);

            long totalSpent = 0;     // 🆕 tổng điểm trừ
            int rewardedCount = 0;   // 🆕 số member được thưởng

            for (Long userId : memberIds) {
                User targetUser = userRepo.findById(userId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));

                double memberMultiplier = getMemberMultiplier(targetUser);
                long totalPoints = Math.round(req.getPoints() * memberMultiplier * clubMultiplier);

                if (clubWallet.getBalancePoints() < totalPoints)
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient club wallet balance.");

                Wallet userWallet = walletService.getOrCreateUserWallet(targetUser);
                walletService.transferPointsWithType(
                        clubWallet,
                        userWallet,
                        totalPoints,
                        req.getReason() + String.format(" (x%.2f×%.2f)", memberMultiplier, clubMultiplier),
                        WalletTransactionTypeEnum.CLUB_TO_MEMBER
                );

                rewardService.sendManualBonusEmail(
                        targetUser.getUserId(),
                        totalPoints,
                        req.getReason(),
                        userWallet.getBalancePoints()
                );

                responses.add(WalletTransactionResponse.builder()
                        .type("CLUB_TO_MEMBER")
                        .amount(totalPoints)
                        .signedAmount("+" + totalPoints)
                        .description(req.getReason() + String.format(" (x%.2f×%.2f)", memberMultiplier, clubMultiplier))
                        .senderName(club.getName())
                        .receiverName(targetUser.getFullName())
                        .createdAt(LocalDateTime.now())
                        .build());

                totalSpent += totalPoints;
                rewardedCount++;
            }

            // ===================================================
            // 📧  NEW: EMAIL TỔNG HỢP CHO LEADER + VICE
            // ===================================================
            List<Membership> leaders = membershipRepo.findByClub_ClubId(club.getClubId());
            for (Membership m : leaders) {
                if (m.getClubRole() == ClubRoleEnum.LEADER ||
                        m.getClubRole() == ClubRoleEnum.VICE_LEADER) {

                    User target = m.getUser();

                    rewardService.sendClubBatchDeductionSummaryEmail(
                            target.getUserId(),
                            club.getName(),
                            totalSpent,
                            rewardedCount,
                            req.getReason()
                    );
                }
            }
        }

        return responses;
    }


}
