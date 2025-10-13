package com.example.uniclub.repository;

import com.example.uniclub.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    boolean existsByUser_UserIdAndClub_ClubId(Long userId, Long clubId);

    List<Membership> findByUser_UserId(Long userId);

    Optional<Membership> findByUser_UserIdAndClub_ClubId(Long userId, Long clubId);
}
