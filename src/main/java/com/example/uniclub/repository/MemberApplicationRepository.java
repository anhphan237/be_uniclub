package com.example.uniclub.repository;

import com.example.uniclub.entity.MemberApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberApplicationRepository extends JpaRepository<MemberApplication, Long> {
    boolean existsByUser_UserIdAndClub_ClubIdAndActiveFlagTrue(Long userId, Long clubId);

}
