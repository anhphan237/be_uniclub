package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubLeaveRequestEntity;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.LeaveRequestStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubLeaveRequestEntityRepository extends JpaRepository<ClubLeaveRequestEntity, Long> {
    boolean existsByMembershipAndStatus(Membership membership, LeaveRequestStatusEnum status);
    List<ClubLeaveRequestEntity> findByMembership_Club_ClubIdOrderByCreatedAtDesc(Long clubId);
    List<ClubLeaveRequestEntity> findByMembership_Club_ClubIdAndStatusOrderByCreatedAtDesc(Long clubId, LeaveRequestStatusEnum status);


}
