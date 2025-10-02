package com.example.uniclub.repository;

import com.example.uniclub.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberApplicationRepository extends JpaRepository<MemberApplication, Long> {
    boolean existsByUserAndClubAndStatus(User user, Club club, ApplicationStatus status);
    Optional<MemberApplication> findFirstByUserAndClubAndStatus(User user, Club club, ApplicationStatus status);
}
