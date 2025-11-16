package com.example.uniclub.repository;

import com.example.uniclub.entity.StaffPerformance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StaffPerformanceRepository extends JpaRepository<StaffPerformance, Long> {

    List<StaffPerformance> findByMembership_MembershipIdAndEvent_DateBetween(
            Long membershipId,
            LocalDate start,
            LocalDate end
    );

    boolean existsByMembership_MembershipIdAndEvent_EventId(Long membershipId, Long eventId);

    List<StaffPerformance> findByMembership_MembershipId(Long membershipId);

    List<StaffPerformance> findByEvent_EventId(Long eventId);

    List<StaffPerformance> findByMembership_Club_ClubIdAndEvent_DateBetween(
            Long clubId,
            LocalDate start,
            LocalDate end
    );

}
