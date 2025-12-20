package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.CashoutResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.CashoutStatusEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
import com.example.uniclub.service.ClubCashoutService;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.example.uniclub.enums.CashoutStatusEnum;

@Service
@RequiredArgsConstructor
public class ClubCashoutServiceImpl implements ClubCashoutService {

    private static final int EXCHANGE_RATE = 100;   // 1 point = 100 VND
    private static final long MIN_POINTS = 1000;

    private final EmailService emailService;
    private final ClubRepository clubRepo;
    private final MembershipRepository membershipRepo;
    private final ClubCashoutRequestRepository cashoutRepo;
    private final WalletService walletService;
    private final UserRepository userRepo;

    // =====================================================
    // 👤 CLUB LEADER – SUBMIT CASHOUT REQUEST
    // =====================================================
    @Override
    @Transactional
    public CashoutResponse requestCashout(
            Long clubId,
            Long points,
            String leaderNote
    ) {
        if (points == null || points < MIN_POINTS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum cashout amount is " + MIN_POINTS + " points"
            );
        }

        CustomUserDetails cud =
                (CustomUserDetails) SecurityContextHolder
                        .getContext().getAuthentication().getPrincipal();

        User leader = userRepo.findById(cud.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "User not found"
                ));

        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Club not found"
                ));

        Membership membership = membershipRepo
                .findByUserAndClub(leader, club)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN, "User is not a member of this club"
                ));

        if (!membership.isLeaderRole()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Only club leader or vice leader can submit a cashout request"
            );
        }

        cashoutRepo.findByClubAndStatus(club, CashoutStatusEnum.PENDING)
                .ifPresent(r -> {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "There is already a pending cashout request for this club"
                    );
                });

        Wallet clubWallet = walletService.getWalletByClubId(clubId);
        if (clubWallet.getBalancePoints() < points) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient club wallet balance"
            );
        }

        long cashAmount = points * EXCHANGE_RATE;

        ClubCashoutRequest request = ClubCashoutRequest.builder()
                .club(club)
                .requestedBy(leader)
                .pointsRequested(points)
                .cashAmount(cashAmount)
                .exchangeRate(EXCHANGE_RATE)
                .leaderNote(leaderNote)
                .status(CashoutStatusEnum.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        cashoutRepo.save(request);

        // 🧾 TRANSACTION LOG (NO BALANCE CHANGE)
        walletService.logTransactionFromSystem(
                clubWallet,
                0,
                WalletTransactionTypeEnum.CASHOUT_REQUEST,
                "Club submitted a cashout request for " + points +
                        " points (Request #" + request.getId() + ")"
        );

        return mapToResponse(request);
    }

    // =====================================================
    // 🧑‍💼 UNIVERSITY STAFF – APPROVE CASHOUT
    // =====================================================
    @Override
    @Transactional
    public void approveCashout(Long requestId, String staffNote) {

        ClubCashoutRequest request = cashoutRepo.findById(requestId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Cashout request not found"
                ));

        if (request.getStatus() != CashoutStatusEnum.PENDING) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Cashout request is not in PENDING status"
            );
        }

        CustomUserDetails cud =
                (CustomUserDetails) SecurityContextHolder
                        .getContext().getAuthentication().getPrincipal();

        User staff = userRepo.findById(cud.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Staff user not found"
                ));

        Wallet clubWallet =
                walletService.getWalletByClubId(
                        request.getClub().getClubId()
                );

        // 1️⃣ Deduct points
        walletService.decrease(
                clubWallet,
                request.getPointsRequested()
        );

        // 2️⃣ Transaction log
        walletService.logTransactionFromSystem(
                clubWallet,
                -request.getPointsRequested(),
                WalletTransactionTypeEnum.CASHOUT_APPROVED,
                "Cashout request approved (Request #" + request.getId() + ")"
        );

        // 3️⃣ Update request
        request.setStatus(CashoutStatusEnum.APPROVED);
        request.setReviewedBy(staff);
        request.setStaffNote(staffNote);
        request.setReviewedAt(LocalDateTime.now());
        cashoutRepo.save(request);

        // 4️⃣ EMAIL – APPROVED
        emailService.sendCashoutApprovedEmail(
                request.getRequestedBy().getEmail(),
                request.getRequestedBy().getFullName(),
                request.getClub().getName(),
                request.getPointsRequested(),
                request.getCashAmount(),
                staffNote
        );
    }

    // =====================================================
    // 🧑‍💼 UNIVERSITY STAFF – REJECT CASHOUT
    // =====================================================
    @Override
    @Transactional
    public void rejectCashout(Long requestId, String staffNote) {

        if (staffNote == null || staffNote.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Rejection reason is required"
            );
        }

        ClubCashoutRequest request = cashoutRepo.findById(requestId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Cashout request not found"
                ));

        if (request.getStatus() != CashoutStatusEnum.PENDING) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Cashout request is not in PENDING status"
            );
        }

        CustomUserDetails cud =
                (CustomUserDetails) SecurityContextHolder
                        .getContext().getAuthentication().getPrincipal();

        User staff = userRepo.findById(cud.getUserId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Staff user not found"
                ));

        request.setStatus(CashoutStatusEnum.REJECTED);
        request.setReviewedBy(staff);
        request.setStaffNote(staffNote);
        request.setReviewedAt(LocalDateTime.now());
        cashoutRepo.save(request);

        Wallet clubWallet =
                walletService.getWalletByClubId(
                        request.getClub().getClubId()
                );

        walletService.logTransactionFromSystem(
                clubWallet,
                0,
                WalletTransactionTypeEnum.CASHOUT_REJECTED,
                "Cashout request rejected (Request #" + request.getId() + ")"
        );

        // 📧 EMAIL – REJECTED
        emailService.sendCashoutRejectedEmail(
                request.getRequestedBy().getEmail(),
                request.getRequestedBy().getFullName(),
                request.getClub().getName(),
                staffNote
        );
    }

    // =====================================================
    // 📜 QUERY
    // =====================================================
    @Override
    public List<CashoutResponse> getCashoutsByClub(Long clubId) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Club not found"
                ));

        return cashoutRepo.findByClubOrderByRequestedAtDesc(club)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CashoutResponse> getPendingCashouts() {
        return cashoutRepo.findByStatusOrderByRequestedAtAsc(
                        CashoutStatusEnum.PENDING
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public List<CashoutResponse> getCashoutsByClubAndStatus(
            Long clubId,
            CashoutStatusEnum status
    ) {
        Club club = clubRepo.findById(clubId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Club not found"
                ));

        return cashoutRepo
                .findByClubAndStatusOrderByRequestedAtDesc(club, status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public CashoutResponse getCashoutDetail(Long id) {
        ClubCashoutRequest request = cashoutRepo.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Cashout request not found"
                ));

        return mapToResponse(request);
    }
    @Override
    public List<CashoutResponse> getApprovedCashouts() {
        return cashoutRepo
                .findByStatusOrderByReviewedAtDesc(
                        CashoutStatusEnum.APPROVED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public List<CashoutResponse> getRejectedCashouts() {
        return cashoutRepo
                .findByStatusOrderByReviewedAtDesc(
                        CashoutStatusEnum.REJECTED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public List<CashoutResponse> getAllCashouts() {
        return cashoutRepo.findAllByOrderByRequestedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =====================================================
    // 🔁 ENTITY → DTO
    // =====================================================
    private CashoutResponse mapToResponse(ClubCashoutRequest r) {
        return CashoutResponse.builder()
                .id(r.getId())
                .clubId(r.getClub().getClubId())
                .clubName(r.getClub().getName())
                .requestedById(r.getRequestedBy().getUserId())
                .requestedByName(r.getRequestedBy().getFullName())
                .pointsRequested(r.getPointsRequested())
                .cashAmount(r.getCashAmount())
                .status(r.getStatus())
                .leaderNote(r.getLeaderNote())
                .staffNote(r.getStaffNote())
                .requestedAt(r.getRequestedAt())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}
