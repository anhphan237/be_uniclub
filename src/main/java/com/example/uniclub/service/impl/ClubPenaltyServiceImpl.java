package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.CreateClubPenaltyRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ClubPenaltyTypeEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubPenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubPenaltyServiceImpl implements ClubPenaltyService {

    private final MembershipRepository membershipRepo;
    private final EventRepository eventRepo;
    private final ClubPenaltyRepository clubPenaltyRepo;
    private final PenaltyRuleRepository ruleRepo;
    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;

    @Override
    @Transactional
    public ClubPenalty createPenalty(Long clubId,
                                     CreateClubPenaltyRequest request,
                                     User createdBy) {

        // 1. Lấy membership
        Membership membership = membershipRepo.findById(request.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership does not belong to this club.");
        }

        // 2. Event (optional)
        Event event = null;
        if (request.eventId() != null) {
            event = eventRepo.findById(request.eventId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        }

        // 3. Rule
        PenaltyRule rule = ruleRepo.findById(request.ruleId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Penalty rule not found"));

        long points = -Math.abs(rule.getPenaltyPoints());  // Member trừ điểm → âm
        long required = Math.abs(points);
        String reason = request.reason() != null ? request.reason() : rule.getDescription();

        // 4. Wallet member
        Wallet memberWallet = walletRepo.findByUser_UserId(membership.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member wallet not found"));

        // 5. Wallet club
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        // 6. Check balance
        if (memberWallet.getBalancePoints() < required) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Member does not have enough points to deduct.");
        }

        // ==========================================================
        // 7. TRỪ ĐIỂM MEMBER
        // ==========================================================
        memberWallet.setBalancePoints(memberWallet.getBalancePoints() - required);
        walletRepo.save(memberWallet);

        txRepo.save(
                WalletTransaction.builder()
                        .wallet(memberWallet)
                        .type(WalletTransactionTypeEnum.MEMBER_PENALTY)
                        .amount(-required)
                        .description("Penalty applied: " + rule.getName())
                        .senderName(memberWallet.getDisplayName())
                        .receiverName(membership.getClub().getName())
                        .build()
        );

        // ==========================================================
        // 8. CỘNG ĐIỂM CHO CLUB
        // ==========================================================
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + required);
        walletRepo.save(clubWallet);

        txRepo.save(
                WalletTransaction.builder()
                        .wallet(clubWallet)
                        .type(WalletTransactionTypeEnum.CLUB_FROM_PENALTY)
                        .amount(required)
                        .description("Penalty income from member: " + memberWallet.getDisplayName())
                        .senderName(memberWallet.getDisplayName())
                        .receiverName(membership.getClub().getName())
                        .build()
        );

        // ==========================================================
        // 9. Lưu bảng club_penalties
        // ==========================================================
        ClubPenalty penalty = ClubPenalty.builder()
                .membership(membership)
                .event(event)
                .type(ClubPenaltyTypeEnum.CLUB_RULE)
                .points((int) points)
                .reason(reason)
                .createdBy(createdBy)
                .build();

        return clubPenaltyRepo.save(penalty);
    }
}
