package com.example.uniclub.repository;

import com.example.uniclub.dto.response.ClubPointsRankingDTO;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // ====================== FINDERS ======================

    Optional<Wallet> findByClub(Club club);
    Optional<Wallet> findByClub_ClubId(Long clubId);
    Optional<Wallet> findByEvent_EventId(Long eventId);
    Optional<Wallet> findByUser(User user);
    Optional<Wallet> findByUser_UserIdAndClub_ClubId(Long userId, Long clubId);

    // ====================== STATISTICS ======================

    @Query("SELECT COALESCE(SUM(w.balancePoints), 0) FROM Wallet w")
    long sumAllPoints();

    @Query("""
        SELECT COALESCE(SUM(w.balancePoints), 0)
        FROM Wallet w
        WHERE w.club.clubId = :clubId
    """)
    long sumPointsByClub(@Param("clubId") Long clubId);
    @Query("""
    SELECT new com.example.uniclub.dto.response.ClubPointsRankingDTO(
        w.club.clubId,
        w.club.name,
        COALESCE(SUM(w.balancePoints), 0)
    )
    FROM Wallet w
    WHERE w.ownerType = com.example.uniclub.enums.WalletOwnerTypeEnum.CLUB
    GROUP BY w.club.clubId, w.club.name
    ORDER BY COALESCE(SUM(w.balancePoints), 0) DESC
""")
    List<ClubPointsRankingDTO> findClubPointsRanking();

}
