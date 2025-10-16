package com.example.uniclub.repository;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    boolean existsByUser_UserIdAndClub_ClubId(Long userId, Long clubId);

    List<Membership> findByUser_UserId(Long userId);

    List<Membership> findByClub_ClubIdAndState(Long clubId, MembershipStateEnum state);

    Optional<Membership> findByUser_UserIdAndClub_ClubId(Long userId, Long clubId);

    // ✅ Dùng trong ClubServiceImpl để tìm leader
    List<Membership> findByClub_ClubIdAndClubRole(Long clubId, ClubRoleEnum clubRole);

    // ✅ Dùng trong EventServiceImpl để tìm leader hoặc vice-leader được duyệt
    Optional<Membership> findFirstByClub_ClubIdAndClubRoleAndState(
            Long clubId, ClubRoleEnum clubRole, MembershipStateEnum state);

    // ✅ Kiểm tra ràng buộc số lượng role trong CLB
    long countByClub_ClubIdAndClubRole(Long clubId, ClubRoleEnum clubRole);

    boolean existsByClub_ClubIdAndClubRole(Long clubId, ClubRoleEnum clubRole);
    Optional<Membership> findFirstByUser_FullNameAndClubRole(String fullName, ClubRoleEnum clubRole);



}
