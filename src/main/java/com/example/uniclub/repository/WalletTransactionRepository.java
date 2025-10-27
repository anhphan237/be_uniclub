package com.example.uniclub.repository;

import com.example.uniclub.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // 🔹 Lịch sử của một ví cụ thể (dành cho club hoặc user)
    List<WalletTransaction> findByWallet_WalletIdOrderByCreatedAtDesc(Long walletId);

    // 🔹 Lịch sử nạp điểm từ UniStaff → Club
    @Query("""
        SELECT tx FROM WalletTransaction tx
        JOIN tx.wallet w
        WHERE w.ownerType = 'CLUB'
          AND tx.description LIKE '%Top-up%'
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findTopupFromUniStaff();

    // 🔹 Lịch sử thưởng điểm từ Club → Member
    @Query("""
        SELECT tx FROM WalletTransaction tx
        JOIN tx.wallet w
        WHERE w.ownerType = 'USER'
          AND tx.description LIKE '%[IN]%'
        ORDER BY tx.createdAt DESC
    """)
    List<WalletTransaction> findRewardToMembers();
}
