package com.example.uniclub.repository;

import com.example.uniclub.entity.MemberApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.example.uniclub.entity.User;

public interface MemberApplicationRepository extends JpaRepository<MemberApplication, Long> {
    boolean existsByUser_UserIdAndClub_ClubIdAndActiveFlagTrue(Long userId, Long clubId);
    List<MemberApplication> findByUser(User user);
    List<MemberApplication> findAllByClub_ClubId(Long clubId);
}
