// NEW
package com.example.uniclub.repository;

import com.example.uniclub.entity.MemberMonthlyActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberMonthlyActivityRepository extends JpaRepository<MemberMonthlyActivity, Long> {

    Optional<MemberMonthlyActivity> findByMembership_MembershipIdAndMonth(Long membershipId, String month);

    List<MemberMonthlyActivity> findByMembership_Club_ClubIdAndMonth(Long clubId, String month);

    List<MemberMonthlyActivity> findByMembership_MembershipIdOrderByMonthDesc(Long membershipId);
}
