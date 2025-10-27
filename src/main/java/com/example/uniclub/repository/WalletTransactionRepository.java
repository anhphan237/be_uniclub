package com.example.uniclub.repository;

import com.example.uniclub.entity.WalletTransaction;
import com.example.uniclub.enums.WalletTransactionTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // 🔹 Lịch sử của một ví cụ thể
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId);

    // 🔹 Lịch sử nạp điểm từ UniStaff → Club
    @Query("""
        SELECT tx FROM WalletTransaction tx
        WHERE tx.type = com.example.uniclub.enums.WalletTransactionTypeEnum.UNI_TO_CLUB
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findTopupFromUniStaff();

    // 🔹 Lịch sử thưởng điểm từ Club → Member
    @Query("""
        SELECT tx FROM WalletTransaction tx
        WHERE tx.type = com.example.uniclub.enums.WalletTransactionTypeEnum.CLUB_TO_MEMBER
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findRewardToMembers();

}
