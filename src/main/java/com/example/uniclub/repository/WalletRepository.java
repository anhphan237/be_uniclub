package com.example.uniclub.repository;

import com.example.uniclub.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUser_UserId(Long userId);
    Optional<Wallet> findByClub_ClubId(Long clubId);

    @Query("SELECT COALESCE(SUM(w.balancePoints), 0) FROM Wallet w")
    long sumAllPoints();

    @Query("SELECT COALESCE(SUM(w.balancePoints), 0) FROM Wallet w WHERE w.club.clubId = :clubId")
    long sumPointsByClub(Long clubId);

    @Query("""
        SELECT w.club.clubId, w.club.name, COALESCE(SUM(w.balancePoints), 0)
        FROM Wallet w
        WHERE w.club IS NOT NULL
        GROUP BY w.club.clubId, w.club.name
        ORDER BY SUM(w.balancePoints) DESC
    """)
    List<Object[]> findClubPointsRanking();
}

