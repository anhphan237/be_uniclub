package com.example.uniclub.service;

import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.WalletTransactionTypeEnum;

import java.util.List;

public interface WalletService {

    // 🔍 Lấy ví theo loại
    Wallet getWalletByClubId(Long clubId);

    Wallet getWalletById(Long walletId);

    // 🏗️ Tạo ví nếu chưa có
    Wallet getOrCreateClubWallet(Club club);
//    Wallet getOrCreateMembershipWallet(Membership membership);
    Wallet getOrCreateUserWallet(User user);
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
    List<WalletTransactionResponse> findAllUniToEventTransactions();


    WalletTransaction topupPointsFromUniversity(Wallet clubWallet, long points, String reason);

    // 🏫 Nạp điểm cho CLB có tên người thực hiện (University Staff)
    void topupPointsFromUniversityWithOperator(Long walletId, long points, String description, String operatorName);
    // ================================================================
// 💸 HÀM MỚI: CHUYỂN ĐIỂM VỚI TYPE CỤ THỂ (VD: CLUB_TO_MEMBER)
// ================================================================
    void transferPointsWithType(
            Wallet sender,
            Wallet receiver,
            long amount,
            String reason,
            WalletTransactionTypeEnum type
    );

    // ================================================================
// 🧾 HÀM MỚI: GHI TRANSACTION TỪ HỆ THỐNG
// ================================================================
    void logTransactionFromSystem(
            Wallet wallet,
            long amount,
            WalletTransactionTypeEnum type,
            String reason
    );

}
