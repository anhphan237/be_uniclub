package com.example.uniclub.controller;

import com.example.uniclub.dto.ApiResponse;
import com.example.uniclub.dto.response.*;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.repository.WalletTransactionRepository;
import com.example.uniclub.security.JwtUtil;
import com.example.uniclub.service.ClubMonthlyActivityService;
import com.example.uniclub.service.MemberActivityQueryService;
import com.example.uniclub.service.MemberActivityService;
import com.example.uniclub.service.impl.ClubEventActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Club Activity", description = "Xem và tính toán mức độ hoạt động của member trong CLB")
public class ClubActivityController {

    private final JwtUtil jwtUtil;
    private final MembershipRepository membershipRepo;
    private final MemberActivityService memberActivityService;
    private final ClubEventActivityService clubEventActivityService;
    private final MemberActivityQueryService memberActivityQueryService;
    private final ClubMonthlyActivityService activityService;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository walletTransactionRepo;

    // ==================== HELPER ====================
    private YearMonth parseYearMonth(Integer year, Integer month, boolean defaultPrevMonth) {
        if (year != null && month != null) return YearMonth.of(year, month);

        YearMonth now = YearMonth.now();
        return defaultPrevMonth ? now.minusMonths(1) : now;
    }

    // ==================== MEMBER ACTIVITY ====================
    @GetMapping("/{clubId}/activities/monthly")
    public ResponseEntity<ApiResponse<ClubActivityMonthlyResponse>> getClubActivityMonthly(
            @PathVariable Long clubId, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month, HttpServletRequest request) {

        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        YearMonth ym = parseYearMonth(year, month, false);
        return ResponseEntity.ok(ApiResponse.ok(
                memberActivityQueryService.getClubActivity(clubId, ym)
        ));
    }

    @GetMapping("/memberships/{membershipId}/activity")
    public ResponseEntity<ApiResponse<MemberActivityDetailResponse>> getMemberActivityDetail(
            @PathVariable Long membershipId, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month, HttpServletRequest request) {

        User current = jwtUtil.getUserFromRequest(request);

        Membership membership = membershipRepo.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        ensureClubManagePermission(current, membership.getClub().getClubId());

        YearMonth ym = parseYearMonth(year, month, false);
        return ResponseEntity.ok(ApiResponse.ok(
                memberActivityQueryService.getMemberActivity(membershipId, ym)
        ));
    }

    @PostMapping("/{clubId}/activities/recalculate")
    public ResponseEntity<ApiResponse<String>> recalculateClubActivity(
            @PathVariable Long clubId, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month, HttpServletRequest request) {

        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        YearMonth ym = parseYearMonth(year, month, true);

        memberActivityService.recalculateForClubAndMonth(clubId, ym);
        return ResponseEntity.ok(ApiResponse.ok("Recalculated for " + ym));
    }

    // ==================== PERMISSION ====================
    private void ensureClubManagePermission(User user, Long clubId) {

        String roleName = user.getRole().getRoleName();

        // UniStaff or Admin luôn có quyền
        if ("ADMIN".equalsIgnoreCase(roleName) ||
                "UNIVERSITY_STAFF".equalsIgnoreCase(roleName)) return;

        // Leader / Vice-leader
        Membership membership = membershipRepo
                .findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club."));

        if (!(membership.getClubRole() == ClubRoleEnum.LEADER ||
                membership.getClubRole() == ClubRoleEnum.VICE_LEADER)) {

            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You are not leader/vice-leader of this club.");
        }

        if (membership.getState() != MembershipStateEnum.ACTIVE &&
                membership.getState() != MembershipStateEnum.APPROVED) {

            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your membership is not active.");
        }
    }

    // ==================== EVENT ACTIVITY (UNISTAFF) ====================
    @GetMapping("/{clubId}/event-activity/monthly")
    public ResponseEntity<ApiResponse<ClubEventMonthlyActivityResponse>> getClubEventActivity(
            @PathVariable Long clubId, @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month, HttpServletRequest request) {

        User current = jwtUtil.getUserFromRequest(request);

        String role = current.getRole().getRoleName();
        if (!role.equalsIgnoreCase("ADMIN") &&
                !role.equalsIgnoreCase("UNIVERSITY_STAFF")) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only UniStaff/Admin can view event activity.");
        }

        YearMonth ym = parseYearMonth(year, month, false);
        return ResponseEntity.ok(ApiResponse.ok(
                clubEventActivityService.getClubEventActivity(clubId, ym)
        ));
    }

    // ==================== REWARD LOGIC ====================

    @PostMapping("/{clubId}/members/reward")
    public ResponseEntity<ApiResponse<String>> distributeRewards(
            @PathVariable Long clubId, @RequestParam int year,
            @RequestParam int month, HttpServletRequest request) {

        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        activityService.distributeRewardToMembers(clubId, year, month);

        return ResponseEntity.ok(ApiResponse.ok(
                "Reward distributed for " + month + "/" + year
        ));
    }

    @GetMapping("/{clubId}/members/me/reward")
    public ResponseEntity<ApiResponse<MemberRewardMonthlyResponse>> getMyMonthlyReward(
            @PathVariable Long clubId, @RequestParam int year,
            @RequestParam int month, HttpServletRequest request) {

        User user = jwtUtil.getUserFromRequest(request);

        membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club"));

        Wallet wallet = walletRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        List<WalletTransaction> list =
                walletTransactionRepo.findMonthlyReward(wallet.getWalletId(), year, month);

        long totalReward = list.stream().mapToLong(WalletTransaction::getAmount).sum();

        return ResponseEntity.ok(ApiResponse.ok(
                MemberRewardMonthlyResponse.builder()
                        .year(year).month(month)
                        .totalReward(totalReward)
                        .transactions(list)
                        .build()
        ));
    }

    @GetMapping("/{clubId}/members/me/reward-history")
    public ResponseEntity<ApiResponse<List<WalletTransaction>>> getMyRewardHistory(
            @PathVariable Long clubId, HttpServletRequest request) {

        User user = jwtUtil.getUserFromRequest(request);

        membershipRepo.findByUser_UserIdAndClub_ClubId(user.getUserId(), clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You are not a member of this club"));

        Wallet wallet = walletRepo.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User wallet not found"));

        List<WalletTransaction> list =
                walletTransactionRepo.findByWallet_WalletIdAndType(
                        wallet.getWalletId(),
                        WalletTransactionTypeEnum.BONUS_REWARD
                );


        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // ==================== REWARD SUMMARY (LEADER) ====================
    @GetMapping("/{clubId}/rewards/summary")
    public ResponseEntity<ApiResponse<Object>> getClubRewardSummary(
            @PathVariable Long clubId, @RequestParam int year,
            @RequestParam int month, HttpServletRequest req) {

        User current = jwtUtil.getUserFromRequest(req);
        ensureClubManagePermission(current, clubId);

        List<WalletTransaction> list =
                walletTransactionRepo.findClubSpentForRewards(clubId, year, month);

        long totalSpent = list.stream().mapToLong(WalletTransaction::getAmount).sum();

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                        "clubId", clubId,
                        "year", year,
                        "month", month,
                        "totalSpent", totalSpent,
                        "transactionCount", list.size()
                )
        ));
    }

    // ==================== BREAKDOWN FOR MEMBERS ====================
    @GetMapping("/{clubId}/rewards/breakdown")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRewardBreakdown(
            @PathVariable Long clubId,
            @RequestParam int year,
            @RequestParam int month,
            HttpServletRequest req
    ) {
        User current = jwtUtil.getUserFromRequest(req);
        ensureClubManagePermission(current, clubId);

        List<Membership> members =
                membershipRepo.findByClub_ClubIdAndStateIn(
                        clubId,
                        List.of(MembershipStateEnum.ACTIVE, MembershipStateEnum.APPROVED)
                );

        List<Map<String, Object>> result = members.stream().map(m -> {

            List<WalletTransaction> txs =
                    walletTransactionRepo.findMemberRewardDetail(
                            m.getMembershipId(), year, month
                    );

            long total = txs.stream().mapToLong(WalletTransaction::getAmount).sum();

            return Map.<String, Object>of(
                    "membershipId", m.getMembershipId(),
                    "userName", m.getUser().getFullName(),
                    "studentCode", m.getUser().getStudentCode(),
                    "reward", total,
                    "transactions", txs.stream().map(WalletTransactionResponse::from).toList()
            );

        }).toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }


    // ==================== STATUS ====================
    @GetMapping("/{clubId}/rewards/status")
    public ResponseEntity<ApiResponse<Object>> getRewardStatus(
            @PathVariable Long clubId, @RequestParam int year,
            @RequestParam int month, HttpServletRequest req) {

        User current = jwtUtil.getUserFromRequest(req);
        ensureClubManagePermission(current, clubId);

        long count = walletTransactionRepo.countClubRewardTransactions(clubId, year, month);

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                        "clubId", clubId,
                        "year", year,
                        "month", month,
                        "rewardDistributed", count > 0,
                        "transactionCount", count
                )
        ));
    }

    // ==================== TRANSACTIONS ====================
    @GetMapping("/{clubId}/rewards/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getRewardTransactions(
            @PathVariable Long clubId, @RequestParam int year,
            @RequestParam int month, HttpServletRequest request) {

        User current = jwtUtil.getUserFromRequest(request);
        ensureClubManagePermission(current, clubId);

        List<WalletTransaction> list =
                walletTransactionRepo.findClubSpentForRewards(clubId, year, month);

        return ResponseEntity.ok(ApiResponse.ok(
                list.stream().map(WalletTransactionResponse::from).toList()
        ));
    }
}
