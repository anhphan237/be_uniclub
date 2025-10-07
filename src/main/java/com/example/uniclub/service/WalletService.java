package com.example.uniclub.service;

import com.example.uniclub.entity.Wallet;

public interface WalletService {
    Wallet getWalletByUserId(Long userId);
}
