package com.example.uniclub.repository;

import com.example.uniclub.entity.StaffPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StaffPerformanceRepository extends JpaRepository<StaffPerformance, Long> {

    @Query("""
        SELECT sp FROM StaffPerformance sp
        WHERE sp.membership.membershipId = :membershipId
          AND sp.event.startDate >= :start
          AND sp.event.endDate <= :end
    """)
    List<StaffPerformance> findPerformanceInRange(
            @Param("membershipId") Long membershipId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    boolean existsByMembership_MembershipIdAndEvent_EventId(Long membershipId, Long eventId);

    List<StaffPerformance> findByEvent_EventId(Long eventId);

    List<StaffPerformance> findByMembership_Club_ClubIdAndEvent_StartDateGreaterThanEqualAndEvent_EndDateLessThanEqual(
            Long clubId,
            LocalDate start,
            LocalDate end
    );
    @Query("""
    SELECT AVG(
        CASE 
            WHEN s.performance = 'POOR' THEN 40
            WHEN s.performance = 'AVERAGE' THEN 70
            WHEN s.performance = 'GOOD' THEN 90
            WHEN s.performance = 'EXCELLENT' THEN 100
            ELSE 50
        END
    )
    FROM StaffPerformance s
    WHERE s.event.hostClub.clubId = :clubId
      AND s.event.endDate BETWEEN :start AND :end
""")
    Double avgPerformanceByClub(@Param("clubId") Long clubId,
                                @Param("start") java.time.LocalDate start,
                                @Param("end") java.time.LocalDate end);

}
