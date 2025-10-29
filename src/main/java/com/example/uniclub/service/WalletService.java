package com.example.uniclub.service;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.WalletTransaction;

import java.util.List;

public interface WalletService {

    // 🔍 Lấy ví theo CLB / Membership / ID
    Wallet getWalletByClubId(Long clubId);
    Wallet getWalletByMembershipId(Long membershipId);
    Wallet getWalletById(Long walletId);

    // 💰 Tạo ví nếu chưa có
    Wallet getOrCreateClubWallet(Club club);
    Wallet getOrCreateMembershipWallet(Membership membership);

    // 🔧 Tăng / giảm điểm
    void increase(Wallet wallet, int points);
    void decrease(Wallet wallet, int points);

    // 💸 Tác vụ có log giao dịch
    void addPoints(Wallet wallet, int points, String description);
    void reducePoints(Wallet wallet, int points, String description);
    void transferPoints(Wallet from, Wallet to, int points, String description);

    // 📜 Lịch sử giao dịch
    List<WalletTransaction> getTransactionsByWallet(Long walletId);
    List<WalletTransactionResponse> getAllClubTopups();
    List<WalletTransactionResponse> getAllMemberRewards();

    void logUniToClubTopup(Wallet clubWallet, int points, String reason);
    void logClubToMemberReward(Wallet memberWallet, int points, String reason);
    Wallet getUniversityWallet();
    List<WalletTransactionResponse> getWalletTransactions(Long walletId);

}
