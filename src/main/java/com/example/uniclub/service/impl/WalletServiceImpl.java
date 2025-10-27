package com.example.uniclub.service.impl;

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
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found for clubId: " + clubId));
    }

    @Override
    public Wallet getWalletByMembershipId(Long membershipId) {
        return walletRepo.findByMembership_MembershipId(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found for membershipId: " + membershipId));
    }

    @Override
    public Wallet getWalletById(Long walletId) {
        return walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found: " + walletId));
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
    // 💰 TĂNG / GIẢM ĐIỂM (KHÔNG GHI LOG)
    // ================================================================
    @Override
    @Transactional
    public void increase(Wallet wallet, int points) {
        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);
    }

    @Override
    @Transactional
    public void decrease(Wallet wallet, int points) {
        if (wallet.getBalancePoints() < points)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");
        wallet.setBalancePoints(wallet.getBalancePoints() - points);
        walletRepo.save(wallet);
    }

    // ================================================================
    // 📜 TÁC VỤ CÓ GHI LOG GIAO DỊCH
    // ================================================================
    @Override
    @Transactional
    public void addPoints(Wallet wallet, int points, String description) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");

        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepo.save(wallet);

        txRepo.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.ADD)
                .amount(points)
                .description(description)
                .build());
    }

    @Override
    @Transactional
    public void reducePoints(Wallet wallet, int points, String description) {
        if (points <= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Points must be positive");

        if (wallet.getBalancePoints() < points)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient points");

        wallet.setBalancePoints(wallet.getBalancePoints() - points);
        walletRepo.save(wallet);

        txRepo.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(WalletTransactionTypeEnum.REDUCE)
                .amount(points)
                .description(description)
                .build());
    }

    @Override
    @Transactional
    public void transferPoints(Wallet from, Wallet to, int points, String description) {
        if (from.getWalletId().equals(to.getWalletId()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot transfer to the same wallet");

        reducePoints(from, points, "[OUT] " + description);
        addPoints(to, points, "[IN] " + description);
    }

    // ================================================================
    // 🧾 LỊCH SỬ GIAO DỊCH
    // ================================================================
    @Override
    public List<WalletTransaction> getTransactionsByWallet(Long walletId) {
        return txRepo.findByWallet_WalletIdOrderByCreatedAtDesc(walletId);
    }

    @Override
    public List<WalletTransaction> getAllClubTopups() {
        return txRepo.findTopupFromUniStaff();
    }

    @Override
    public List<WalletTransaction> getAllMemberRewards() {
        return txRepo.findRewardToMembers();
    }
}
