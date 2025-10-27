package com.example.uniclub.service;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.WalletTransaction;

import java.util.List;

public interface WalletService {

    // 🔍 Lấy ví theo User / Club / ID
    Wallet getWalletByUserId(Long userId);
    Wallet getWalletByClubId(Long clubId);
    Wallet getWalletById(Long walletId);

    // 🔧 Tăng / giảm điểm (không log transaction)
    void increase(Wallet wallet, int points);
    void decrease(Wallet wallet, int points);

    // 🏗️ Tự tạo ví nếu chưa có
    Wallet getOrCreateUserWallet(User user);
    Wallet getOrCreateClubWallet(Club club);

    // 💰 Tác vụ logic có log giao dịch (transaction log)
    void addPoints(Wallet wallet, int points, String description);
    void reducePoints(Wallet wallet, int points, String description);
    void transferPoints(Wallet from, Wallet to, int points, String description);

    List<WalletTransaction> getTransactionsByWallet(Long walletId);
    List<WalletTransaction> getAllClubTopups();
    List<WalletTransaction> getAllMemberRewards();

}
