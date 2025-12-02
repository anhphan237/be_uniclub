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



    @Query("""
    SELECT COALESCE(SUM(wt.amount), 0)
    FROM WalletTransaction wt
    JOIN wt.wallet w
    JOIN Event e ON e.wallet.walletId = w.walletId
    LEFT JOIN e.coHostRelations r
    WHERE wt.type = com.example.uniclub.enums.WalletTransactionTypeEnum.EVENT_BUDGET_GRANT
      AND (e.hostClub.clubId = :clubId OR r.club.clubId = :clubId)
""")
    Long sumEventBudgetByClub(@Param("clubId") Long clubId);
    List<WalletTransaction> findByWallet_WalletIdAndType(
            Long walletId,
            WalletTransactionTypeEnum type
    );

    @Query("""
    SELECT t FROM WalletTransaction t
    WHERE t.wallet.walletId = :walletId
      AND t.type = com.example.uniclub.enums.WalletTransactionTypeEnum.BONUS_REWARD
      AND YEAR(t.createdAt) = :year
      AND MONTH(t.createdAt) = :month
    ORDER BY t.createdAt DESC
""")
    List<WalletTransaction> findMonthlyReward(
            @Param("walletId") Long walletId,
            @Param("year") int year,
            @Param("month") int month
    );
    @Query("""
    SELECT t FROM WalletTransaction t
    WHERE t.wallet.club.clubId = :clubId
      AND t.type = com.example.uniclub.enums.WalletTransactionTypeEnum.CLUB_TO_MEMBER
      AND YEAR(t.createdAt) = :year
      AND MONTH(t.createdAt) = :month
    ORDER BY t.createdAt DESC
""")
    List<WalletTransaction> findClubSpentForRewards(
            @Param("clubId") Long clubId,
            @Param("year") int year,
            @Param("month") int month
    );
    @Query("""
    SELECT t FROM WalletTransaction t
    WHERE t.receiverMembership.membershipId = :membershipId
      AND t.type = com.example.uniclub.enums.WalletTransactionTypeEnum.BONUS_REWARD
      AND YEAR(t.createdAt) = :year
      AND MONTH(t.createdAt) = :month
    ORDER BY t.createdAt DESC
""")
    List<WalletTransaction> findMemberRewardDetail(
            @Param("membershipId") Long membershipId,
            @Param("year") int year,
            @Param("month") int month
    );
    @Query("""
    SELECT COUNT(t) FROM WalletTransaction t
    WHERE t.wallet.club.clubId = :clubId
      AND t.type = com.example.uniclub.enums.WalletTransactionTypeEnum.CLUB_TO_MEMBER
      AND YEAR(t.createdAt) = :year
      AND MONTH(t.createdAt) = :month
""")
    long countClubRewardTransactions(
            @Param("clubId") Long clubId,
            @Param("year") int year,
            @Param("month") int month
    );

}
