package com.example.uniclub.repository;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByClub_ClubIdAndState(Long clubId, MembershipStateEnum state);
    long countByState(MembershipStateEnum state);

    @Query("SELECT COUNT(m) FROM Membership m WHERE m.state = :state")
    long countByStateV2(MembershipStateEnum state);

    @Query("SELECT COUNT(m) FROM Membership m WHERE m.club.clubId = :clubId AND m.state = :state")
    long countByClubIdAndState(Long clubId, MembershipStateEnum state);

    // ✅ Lấy tất cả membership của 1 club
    List<Membership> findByClub_ClubId(Long clubId);

    boolean existsByUser_UserIdAndClub_ClubIdAndClubRoleIn(
            Long userId, Long clubId, List<com.example.uniclub.enums.ClubRoleEnum> roles);

    Optional<Membership> findByUser_UserIdAndClubRoleAndState(
            Long userId, ClubRoleEnum clubRole, MembershipStateEnum state);

    // ✅ Dùng trong RedeemServiceImpl để kiểm tra user có phải thành viên ACTIVE của CLB
    Optional<Membership> findByUser_UserIdAndClub_ClubIdAndState(
            Long userId,
            Long clubId,
            MembershipStateEnum state
    );
    int countByUser_UserIdAndState(Long userId, MembershipStateEnum state);

    @Query("SELECT COUNT(DISTINCT m.club.id) FROM Membership m WHERE m.user.id = :userId")
    long countDistinctActiveClubsByUserId(@Param("userId") Long userId);

    Optional<Membership> findByUserAndClub(User user, Club club);



}
