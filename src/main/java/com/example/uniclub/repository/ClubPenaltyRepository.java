// NEW
package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubPenalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ClubPenaltyRepository extends JpaRepository<ClubPenalty, Long> {

    List<ClubPenalty> findByMembership_MembershipIdAndCreatedAtBetween(
            Long membershipId,
            LocalDateTime start,
            LocalDateTime end
    );
}
