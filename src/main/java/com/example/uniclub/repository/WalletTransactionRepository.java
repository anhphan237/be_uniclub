package com.example.uniclub.repository;

import com.example.uniclub.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        LEFT JOIN FETCH w.club c
        LEFT JOIN FETCH tx.receiverUser u
        WHERE tx.type = com.example.uniclub.enums.WalletTransactionTypeEnum.CLUB_TO_MEMBER
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findRewardToMembers();


    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.wallet.user.id = :userId
    """)
    long sumRewardPointsByUserId(@Param("userId") Long userId);


    @Query("""
    SELECT t FROM WalletTransaction t
    JOIN t.wallet w
    WHERE w.ownerType = com.example.uniclub.enums.WalletOwnerTypeEnum.UNIVERSITY
      AND t.type = com.example.uniclub.enums.WalletTransactionTypeEnum.EVENT_BUDGET_GRANT
    ORDER BY t.createdAt DESC
""")
    List<WalletTransaction> findAllUniToEventTransactions();




}
