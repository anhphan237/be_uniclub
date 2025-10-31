package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.repository.WalletTransactionRepository;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;

    // ================================================================
    // 🔍 LẤY VÍ THEO CLUB / MEMBERSHIP / ID
    // ================================================================
    @Override
    public Wallet getWalletByClubId(Long clubId) {
        return walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Wallet not found for clubId: " + clubId));
    }

    @Override
    public Wallet getWalletByMembershipId(Long membershipId) {
        return walletRepo.findByMembership_MembershipId(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Wallet not found for membershipId: " + membershipId));
    }

    @Override
    public Wallet getWalletById(Long walletId) {
        return walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Wallet not found: " + walletId));
    }

    // ================================================================
    // 🏗️ TẠO VÍ NẾU CHƯA CÓ
    // ================================================================
    @Override
    @Transactional
    public Wallet getOrCreateClubWallet(Club club) {
        return walletRepo.findByClub(club)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .club(club)
                        .ownerType(WalletOwnerTypeEnum.CLUB)
                        .balancePoints(0L)
                        .build()));
    }

    @Override
    @Transactional
    public Wallet getOrCreateMembershipWallet(Membership membership) {
        return walletRepo.findByMembership(membership)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .membership(membership)
                        .ownerType(WalletOwnerTypeEnum.MEMBERSHIP)
                        .balancePoints(0L)
                        .build()));
    }

    // ================================================================
    // 💰 TĂNG / GIẢM ĐIỂM
    // ================================================================
    @Override
    @Transactional
    public void increase(Wallet wallet, long points) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");
        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);
    }

    @Override
    @Transactional
    public void decrease(Wallet wallet, long points) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");
        if (wallet.getBalancePoints() < points)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");

        wallet.setBalancePoints(wallet.getBalancePoints() - points);
        walletRepo.save(wallet);
    }

    // ================================================================
    // 📜 GIAO DỊCH CÓ LOG
    // ================================================================
    @Override
    @Transactional
    public void addPoints(Wallet wallet, long points, String description) {
        increase(wallet, points);
        saveTransaction(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.ADD)
                .amount(points)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void reducePoints(Wallet wallet, long points, String description) {
        decrease(wallet, points);
        saveTransaction(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.REDUCE)
                .amount(points)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void transferPoints(Wallet from, Wallet to, long points, String description) {
        if (from.getBalancePoints() < points)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance in source wallet");

        from.setBalancePoints(from.getBalancePoints() - points);
        to.setBalancePoints(to.getBalancePoints() + points);
        walletRepo.save(from);
        walletRepo.save(to);

        // OUT
        saveTransaction(WalletTransaction.builder()
                .wallet(from)
                .type(WalletTransactionTypeEnum.TRANSFER)
                .amount(-points)
                .description("[OUT] " + description)
                .createdAt(LocalDateTime.now())
                .build());

        // IN
        saveTransaction(WalletTransaction.builder()
                .wallet(to)
                .type(WalletTransactionTypeEnum.TRANSFER)
                .amount(points)
                .description("[IN] " + description)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ================================================================
    // 🎓 NGHIỆP VỤ CHUYÊN BIỆT
    // ================================================================
    @Override
    @Transactional
    public void logUniToClubTopup(Wallet clubWallet, long points, String reason) {
        saveTransaction(WalletTransaction.builder()
                .wallet(clubWallet)
                .type(WalletTransactionTypeEnum.UNI_TO_CLUB)
                .amount(points)
                .description(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void logClubToMemberReward(Wallet memberWallet, long points, String reason) {
        saveTransaction(WalletTransaction.builder()
                .wallet(memberWallet)
                .type(WalletTransactionTypeEnum.CLUB_TO_MEMBER)
                .amount(points)
                .description(reason)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ================================================================
    // 🧾 LỊCH SỬ GIAO DỊCH
    // ================================================================
    @Override
    public List<WalletTransaction> getTransactionsByWallet(Long walletId) {
        return txRepo.findByWallet_WalletIdOrderByCreatedAtDesc(walletId);
    }

    @Override
    public List<WalletTransactionResponse> getAllClubTopups() {
        return txRepo.findTopupFromUniStaff().stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<WalletTransactionResponse> getAllMemberRewards() {
        return txRepo.findRewardToMembers().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getWalletTransactions(Long walletId) {
        return txRepo.findByWallet_WalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ================================================================
    // 🧩 AUTO-FILL TÊN GỬI & NHẬN (chuẩn hóa)
    // ================================================================
    @Transactional
    public void saveTransaction(WalletTransaction tx) {
        Wallet w = walletRepo.findById(tx.getWallet().getWalletId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found when saving transaction"));
        tx.setWallet(w);

        // ✅ Receiver = chủ ví
        if (tx.getReceiverName() == null) {
            if (w.getMembership() != null && w.getMembership().getUser() != null)
                tx.setReceiverName(w.getMembership().getUser().getFullName());
            else if (w.getClub() != null)
                tx.setReceiverName(w.getClub().getName());
            else if (w.getEvent() != null)
                tx.setReceiverName(w.getEvent().getName());
            else
                tx.setReceiverName("System");
        }

        // ✅ Sender theo loại giao dịch
        if (tx.getSenderName() == null) {
            switch (tx.getType()) {
                case CLUB_TO_MEMBER -> {
                    if (w.getMembership() != null && w.getMembership().getClub() != null)
                        tx.setSenderName(w.getMembership().getClub().getName());
                    else if (w.getClub() != null)
                        tx.setSenderName(w.getClub().getName());
                    else
                        tx.setSenderName("Club");
                }
                case UNI_TO_CLUB -> tx.setSenderName("University Staff");
                case REDEEM_PRODUCT -> {
                    if (w.getMembership() != null && w.getMembership().getUser() != null)
                        tx.setSenderName(w.getMembership().getUser().getFullName());
                    else
                        tx.setSenderName("Member");
                }
                case REFUND_PRODUCT -> tx.setSenderName("System Refund");
                case TRANSFER -> tx.setSenderName("[System Transfer]");
                case ADD -> tx.setSenderName("System Add");
                case REDUCE -> tx.setSenderName("System Reduce");
                default -> tx.setSenderName("System");
            }
        }

        if (tx.getCreatedAt() == null)
            tx.setCreatedAt(LocalDateTime.now());

        txRepo.save(tx);
    }

    // ================================================================
    // 🧾 MAP TRANSACTION → RESPONSE
    // ================================================================
    private WalletTransactionResponse mapToResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .senderName(tx.getSenderName())
                .receiverName(tx.getReceiverName())
                .build();
    }

    // ================================================================
    // 🏫 NẠP ĐIỂM TỪ UNIVERSITY
    // ================================================================
    @Override
    @Transactional
    public void topupPointsFromUniversity(Wallet targetWallet, long points, String description) {
        targetWallet.setBalancePoints(targetWallet.getBalancePoints() + points);
        walletRepo.save(targetWallet);

        saveTransaction(WalletTransaction.builder()
                .wallet(targetWallet)
                .type(WalletTransactionTypeEnum.UNI_TO_CLUB)
                .amount(points)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build());
    }
    @Override
    @Transactional
    public void topupPointsFromUniversityWithOperator(Long walletId, long points, String description, String operatorName) {
        Wallet targetWallet = walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found: " + walletId));

        targetWallet.setBalancePoints(targetWallet.getBalancePoints() + points);
        walletRepo.save(targetWallet);

        saveTransaction(WalletTransaction.builder()
                .wallet(targetWallet)
                .type(WalletTransactionTypeEnum.UNI_TO_CLUB)
                .amount(points)
                .description(description)
                .senderName(operatorName != null ? operatorName : "University Staff") // ✅ Ghi tên người thật
                .createdAt(LocalDateTime.now())
                .build());
    }


}
