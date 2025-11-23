package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.CreateClubPenaltyRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ClubPenaltyTypeEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubPenaltyService;
import com.example.uniclub.service.EmailService;
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
    private final EmailService emailService;

    @Override
    @Transactional
    public ClubPenalty createPenalty(Long clubId,
                                     CreateClubPenaltyRequest request,
                                     User createdBy) {

        // 1. Lấy membership trong CLB
        Membership membership = membershipRepo.findById(request.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership does not belong to this club.");
        }

        // 2. Rule phạt
        PenaltyRule rule = ruleRepo.findById(request.ruleId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Penalty rule not found"));

        long penaltyPoints = Math.abs(rule.getPenaltyPoints());
        String reason = (request.reason() != null && !request.reason().isBlank())
                ? request.reason()
                : rule.getDescription();

        // 3. Ví member
        Wallet memberWallet = walletRepo.findByUser_UserId(membership.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member wallet not found"));

        // 4. Ví club
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        // 5. Check đủ điểm để trừ
        if (memberWallet.getBalancePoints() < penaltyPoints) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Member does not have enough points.");
        }

        // ======================
        // 6. TRỪ ĐIỂM MEMBER
        // ======================
        memberWallet.setBalancePoints(memberWallet.getBalancePoints() - penaltyPoints);
        walletRepo.save(memberWallet);

        txRepo.save(
                WalletTransaction.builder()
                        .wallet(memberWallet)
                        .type(WalletTransactionTypeEnum.MEMBER_PENALTY)
                        .amount(-penaltyPoints)
                        .description("Penalty applied: " + rule.getName())
                        .senderName(memberWallet.getDisplayName())
                        .receiverName(membership.getClub().getName())
                        .build()
        );

        // ======================
        // 7. CỘNG ĐIỂM CLUB
        // ======================
        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + penaltyPoints);
        walletRepo.save(clubWallet);

        txRepo.save(
                WalletTransaction.builder()
                        .wallet(clubWallet)
                        .type(WalletTransactionTypeEnum.CLUB_FROM_PENALTY)
                        .amount(penaltyPoints)
                        .description("Penalty income from member: " + memberWallet.getDisplayName())
                        .senderName(memberWallet.getDisplayName())
                        .receiverName(membership.getClub().getName())
                        .build()
        );

        // ======================
        // 8. LƯU BẢNG PHIẾU PHẠT — KHÔNG CÓ EVENT
        // ======================
        ClubPenalty penalty = ClubPenalty.builder()
                .membership(membership)
                .type(ClubPenaltyTypeEnum.CLUB_RULE)
                .points((int) -penaltyPoints)
                .reason(reason)
                .createdBy(createdBy)
                .build();

        ClubPenalty saved = clubPenaltyRepo.save(penalty);

        // ======================
        // 9. GỬI EMAIL CHO MEMBER
        // ======================
        emailService.sendPenaltyNotificationEmail(
                membership.getUser(),
                membership.getClub(),
                rule,
                saved
        );

        return saved;
    }
}