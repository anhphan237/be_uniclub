package com.example.uniclub.repository;

import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberMonthlyActivityRepository extends JpaRepository<MemberMonthlyActivity, Long> {

    // Tìm 1 record theo membership + year + month (dùng cho ActivityEngine & Query)
    Optional<MemberMonthlyActivity> findByMembershipAndYearAndMonth(
            Membership membership,
            Integer year,
            Integer month
    );

    // Dùng trong QueryService: get activity 1 membership
    Optional<MemberMonthlyActivity> findByMembership_MembershipIdAndYearAndMonth(
            Long membershipId,
            Integer year,
            Integer month
    );

    // Dùng trong QueryService: get list activity của 1 club 1 tháng
    List<MemberMonthlyActivity> findByMembership_Club_ClubIdAndYearAndMonth(
            Long clubId,
            Integer year,
            Integer month
    );

    // Dùng cho ranking toàn trường 1 tháng
    List<MemberMonthlyActivity> findByYearAndMonth(Integer year, Integer month);

    // Lịch sử theo membership (ví dụ cho API “history”)
    List<MemberMonthlyActivity> findByMembership_MembershipIdOrderByYearDescMonthDesc(Long membershipId);
}
