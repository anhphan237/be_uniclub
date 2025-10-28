package com.example.uniclub.repository;

import com.example.uniclub.dto.response.ClubPointsRankingDTO;
import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.Wallet;
import com.example.uniclub.enums.WalletOwnerTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // ====================== FINDERS ======================

    // 🔍 Tìm ví theo CLB (OneToOne)
    Optional<Wallet> findByClub(Club club);

    // 🔍 Tìm ví theo Membership (OneToOne)
    Optional<Wallet> findByMembership(Membership membership);

    // 🔍 Tìm ví theo ID CLB
    Optional<Wallet> findByClub_ClubId(Long clubId);

    // 🔍 Tìm ví theo ID Membership
    Optional<Wallet> findByMembership_MembershipId(Long membershipId);

    // 🔍 Tìm ví theo ID Event
    Optional<Wallet> findByEvent_EventId(Long eventId);


    // ====================== STATISTICS ======================

    // 📊 Tổng điểm toàn hệ thống
    @Query("SELECT COALESCE(SUM(w.balancePoints), 0) FROM Wallet w")
    long sumAllPoints();

    // 📊 Tổng điểm của 1 CLB (gộp cả ví CLB + member trong CLB)
    @Query("""
        SELECT COALESCE(SUM(w.balancePoints), 0)
        FROM Wallet w
        WHERE w.club.clubId = :clubId
           OR (w.membership.club.clubId = :clubId)
    """)
    long sumPointsByClub(@Param("clubId") Long clubId);

    @Query("""
    SELECT new com.example.uniclub.dto.response.ClubPointsRankingDTO(
        w.membership.club.clubId,
        w.membership.club.name,
        COALESCE(SUM(w.balancePoints), 0)
    )
    FROM Wallet w
    WHERE w.membership.club IS NOT NULL
    GROUP BY w.membership.club.clubId, w.membership.club.name
    ORDER BY COALESCE(SUM(w.balancePoints), 0) DESC
""")
    List<ClubPointsRankingDTO> findClubPointsRanking();
    Optional<Wallet> findByOwnerTypeAndClub_Name(WalletOwnerTypeEnum ownerType, String clubName);

}
