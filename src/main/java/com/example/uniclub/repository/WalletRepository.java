package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // 🔍 Tìm ví theo CLB (OneToOne)
    Optional<Wallet> findByClub(Club club);

    // 🔍 Tìm ví theo Membership (OneToOne)
    Optional<Wallet> findByMembership(Membership membership);

    // 🔍 Tìm ví theo ID CLB
    Optional<Wallet> findByClub_ClubId(Long clubId);

    // 🔍 Tìm ví theo ID Membership
    Optional<Wallet> findByMembership_MembershipId(Long membershipId);

    // 📊 Tổng số điểm toàn hệ thống
    @Query("SELECT COALESCE(SUM(w.balancePoints), 0) FROM Wallet w")
    long sumAllPoints();

    // 📊 Tổng điểm của một CLB (từ ví CLB)
    @Query("SELECT COALESCE(SUM(w.balancePoints), 0) FROM Wallet w WHERE w.club.clubId = :clubId")
    long sumPointsByClub(Long clubId);

    // 📈 Xếp hạng CLB theo tổng điểm
    @Query("""
        SELECT w.club.clubId, w.club.name, COALESCE(SUM(w.balancePoints), 0)
        FROM Wallet w
        WHERE w.club IS NOT NULL
        GROUP BY w.club.clubId, w.club.name
        ORDER BY SUM(w.balancePoints) DESC
    """)
    List<Object[]> findClubPointsRanking();
}
