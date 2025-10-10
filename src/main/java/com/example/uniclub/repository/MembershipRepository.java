package com.example.uniclub.repository;

import com.example.uniclub.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    boolean existsByUserAndClub(User user, Club club);
    Optional<Membership> findByUserAndClub(User user, Club club);

    List<Membership> findAllByUser_UserId(Long userId);

    // 🔹 Thêm hàm này để list tất cả member theo clubId
    List<Membership> findAllByClub_ClubId(Long clubId);

    List<Membership> findAllByClub_ClubIdAndStaffTrue(Long clubId);

    boolean existsByUser_UserIdAndStaffTrue(Long userId);
}
