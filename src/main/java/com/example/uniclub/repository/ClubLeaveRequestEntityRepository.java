package com.example.uniclub.repository;

import com.example.uniclub.entity.ClubLeaveRequestEntity;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.LeaveRequestStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubLeaveRequestEntityRepository extends JpaRepository<ClubLeaveRequestEntity, Long> {
    boolean existsByMembershipAndStatus(Membership membership, LeaveRequestStatusEnum status);
}
