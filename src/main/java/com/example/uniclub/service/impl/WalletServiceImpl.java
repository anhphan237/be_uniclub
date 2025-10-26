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

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found for userId: " + userId));
    }

    @Override
    public Wallet getWalletByClubId(Long clubId) {
        return walletRepo.findByClub_ClubId(clubId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found for clubId: " + clubId));
    }

    @Override
    public Wallet getWalletById(Long walletId) {
        return walletRepo.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found: " + walletId));
    }

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

    @Override
    @Transactional
    public Wallet getOrCreateUserWallet(User user) {
        return walletRepo.findByUser(user)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .user(user)
                        .ownerType(WalletOwnerTypeEnum.USER)
                        .balancePoints(0)
                        .build()));
    }

    @Override
    @Transactional
    public Wallet getOrCreateClubWallet(Club club) {
        return walletRepo.findByClub(club)
                .orElseGet(() -> walletRepo.save(Wallet.builder()
                        .club(club)
                        .ownerType(WalletOwnerTypeEnum.CLUB)
                        .balancePoints(0)
                        .build()));
    }

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
}
