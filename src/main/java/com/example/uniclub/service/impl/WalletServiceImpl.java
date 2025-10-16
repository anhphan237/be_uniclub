package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Wallet;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.WalletRepository;
import com.example.uniclub.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    @Override
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found for userId: " + userId));
    }

    @Override
    public Wallet getWalletById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Wallet not found: " + walletId));
    }

    @Override @Transactional
    public void increase(Wallet wallet, int points) {
        wallet.setBalancePoints(wallet.getBalancePoints() + points);
        walletRepository.save(wallet);
    }

    @Override @Transactional
    public void decrease(Wallet wallet, int points) {
        if (wallet.getBalancePoints() < points) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");
        }
        wallet.setBalancePoints(wallet.getBalancePoints() - points);
        walletRepository.save(wallet);
    }
}
