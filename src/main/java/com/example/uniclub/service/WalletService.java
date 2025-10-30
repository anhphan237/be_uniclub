package com.example.uniclub.service;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.entity.WalletTransaction;
import java.util.List;

public interface WalletService {

    // 🔍 Lấy ví theo loại
    Wallet getWalletByClubId(Long clubId);
    Wallet getWalletByMembershipId(Long membershipId);
    Wallet getWalletById(Long walletId);

    // 🏗️ Tạo ví nếu chưa có
    Wallet getOrCreateClubWallet(Club club);
    Wallet getOrCreateMembershipWallet(Membership membership);

    // 💰 Thao tác tăng / giảm điểm
    void increase(Wallet wallet, long points);
    void decrease(Wallet wallet, long points);

    // 💸 Thao tác có log
    void addPoints(Wallet wallet, long points, String description);
    void reducePoints(Wallet wallet, long points, String description);
    void transferPoints(Wallet from, Wallet to, long points, String description);

    // 🎓 Nghiệp vụ cấp / thưởng điểm
    void logUniToClubTopup(Wallet clubWallet, long points, String reason);
    void logClubToMemberReward(Wallet memberWallet, long points, String reason);
//    Wallet getUniversityWallet();

    // 📜 Lịch sử giao dịch
    List<WalletTransaction> getTransactionsByWallet(Long walletId);
    List<WalletTransactionResponse> getWalletTransactions(Long walletId);
    List<WalletTransactionResponse> getAllClubTopups();
    List<WalletTransactionResponse> getAllMemberRewards();

    void topupPointsFromUniversity(Wallet targetWallet, long points, String description);

}
