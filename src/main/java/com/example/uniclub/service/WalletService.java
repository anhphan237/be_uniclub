package com.example.uniclub.service;

import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.User;

public interface WalletService {
    Wallet getWalletByUserId(Long userId);
    Wallet getWalletById(Long walletId);
    void increase(Wallet wallet, int points);
    void decrease(Wallet wallet, int points);
    Wallet getWalletByClubId(Long clubId);

}
