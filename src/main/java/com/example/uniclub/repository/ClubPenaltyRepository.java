// NEW
package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ClubPenaltyRepository extends JpaRepository<ClubPenalty, Long> {

    List<ClubPenalty> findByMembership_MembershipIdAndCreatedAtBetween(
            Long membershipId,
            LocalDateTime start,
            LocalDateTime end
    );
    @Query("""
    SELECT COALESCE(SUM(p.points), 0)
    FROM ClubPenalty p
    WHERE p.membership.membershipId = :membershipId
      AND p.createdAt BETWEEN :start AND :end
""")
    int sumPenaltyPoints(
            Long membershipId,
            LocalDateTime start,
            LocalDateTime end
    );

}
