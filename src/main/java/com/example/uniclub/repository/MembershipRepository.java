package com.example.uniclub.repository;

import com.example.uniclub.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    boolean existsByUserAndClub(User user, Club club);
    Optional<Membership> findByUserAndClub(User user, Club club);
}
