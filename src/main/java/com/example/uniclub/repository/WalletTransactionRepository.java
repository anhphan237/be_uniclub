package com.example.uniclub.repository;

import com.example.uniclub.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // 🔹 Lịch sử của một ví cụ thể
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId);

    // 🔹 Lịch sử nạp điểm từ UniStaff → Club (FETCH JOIN để lấy luôn Club name)
    @Query("""
        SELECT tx FROM WalletTransaction tx
        LEFT JOIN FETCH tx.wallet w
        LEFT JOIN FETCH w.club c
        WHERE tx.type = com.example.uniclub.enums.WalletTransactionTypeEnum.UNI_TO_CLUB
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findTopupFromUniStaff();

    // 🔹 Lịch sử thưởng điểm từ Club → Member (FETCH JOIN để lấy luôn User name)
    @Query("""
        SELECT tx FROM WalletTransaction tx
        LEFT JOIN FETCH tx.wallet w
        LEFT JOIN FETCH w.membership m
        LEFT JOIN FETCH m.user u
        WHERE tx.type = com.example.uniclub.enums.WalletTransactionTypeEnum.CLUB_TO_MEMBER
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findRewardToMembers();
}
