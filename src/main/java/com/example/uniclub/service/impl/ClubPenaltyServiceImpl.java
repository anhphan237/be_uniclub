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

        // 2. Lấy event (optional)
        Event event = null;
        if (request.eventId() != null) {
            event = eventRepo.findById(request.eventId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));
        }

        // 3. Lấy rule
        PenaltyRule rule = ruleRepo.findById(request.ruleId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Penalty rule not found"));

        long points = rule.getPenaltyPoints(); // luôn âm
        String reason = request.reason() != null ? request.reason() : rule.getDescription();

        // ==========================
        // 4. LẤY WALLET MEMBER
        // ==========================
        Wallet memberWallet = walletRepo.findByUser_UserId(membership.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member wallet not found"));

        // ==========================
        // 5. LẤY WALLET CLUB
        // ==========================
        Wallet clubWallet = walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Club wallet not found"));

        // ==========================
        // 6. TRỪ ĐIỂM MEMBER
        // ==========================
        memberWallet.setBalancePoints(memberWallet.getBalancePoints() + points);
        walletRepo.save(memberWallet);

        txRepo.save(
                WalletTransaction.builder()
                        .wallet(memberWallet)
                        .type(WalletTransactionTypeEnum.MEMBER_PENALTY)
                        .amount(points) // âm
                        .description("Penalty applied: " + rule.getName())
                        .receiverClub(membership.getClub())
                        .senderName(memberWallet.getDisplayName())
                        .receiverName(membership.getClub().getName())
                        .build()
        );

        // ==========================
        // 7. CỘNG ĐIỂM CHO CLUB
        // ==========================
        long clubGain = Math.abs(points);

        clubWallet.setBalancePoints(clubWallet.getBalancePoints() + clubGain);
        walletRepo.save(clubWallet);

        txRepo.save(
                WalletTransaction.builder()
                        .wallet(clubWallet)
                        .type(WalletTransactionTypeEnum.CLUB_FROM_PENALTY)
                        .amount(clubGain) // dương
                        .description("Penalty income from member: " + memberWallet.getDisplayName())
                        .receiverClub(membership.getClub())
                        .senderName(memberWallet.getDisplayName())
                        .receiverName(membership.getClub().getName())
                        .build()
        );

        // ==========================
        // 8. LƯU RECORD PENALTY
        // ==========================
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
