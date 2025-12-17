package com.example.uniclub.repository;

import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // ============================================================
    // 🔍 BASIC LOOKUP
    // ============================================================
    Optional<Event> findByCheckInCode(String checkInCode);

    List<Event> findByHostClub_ClubId(Long clubId);

    List<Event> findByStatus(EventStatusEnum status);

    long countByStatus(EventStatusEnum status);

    long countByHostClub_ClubId(Long clubId);

    List<Event> findAllByStatus(EventStatusEnum status);

    long countByHostClub_ClubIdAndStatus(Long clubId, EventStatusEnum status);

    // ============================================================
    // 🔍 TEXT SEARCH (KHÔNG DÍNH NGÀY)
    // ============================================================
    Page<Event> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Event> findByNameContainingIgnoreCaseAndStatus(
            String name, EventStatusEnum status, Pageable pageable
    );
    @Query("""
    SELECT DISTINCT e FROM Event e
    LEFT JOIN FETCH e.days d
    LEFT JOIN FETCH e.coHostRelations rel
    LEFT JOIN FETCH rel.club
    LEFT JOIN FETCH e.hostClub hc
    LEFT JOIN FETCH e.location l
    WHERE l.locationId = :locationId
    ORDER BY e.startDate ASC
""")
    List<Event> findFullEventsByLocationId(@Param("locationId") Long locationId);
    @Query("""
    SELECT DISTINCT e FROM Event e
    LEFT JOIN FETCH e.days d
    LEFT JOIN FETCH e.hostClub hc
    LEFT JOIN FETCH e.location l
    WHERE l.locationId = :locationId
      AND e.status IN (
        com.example.uniclub.enums.EventStatusEnum.APPROVED,
        com.example.uniclub.enums.EventStatusEnum.ONGOING,
        com.example.uniclub.enums.EventStatusEnum.COMPLETED
      )
    ORDER BY e.startDate ASC
""")
    List<Event> findEventsByLocationWithDays(@Param("locationId") Long locationId);


    // ============================================================
    // 🔥 FILTER MULTI-DAY (NAME + DATE + STATUS)
    // ============================================================
    @Query("""
        SELECT DISTINCT e FROM Event e
        LEFT JOIN e.days d
        WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%'))
          AND (:status IS NULL OR e.status = :status)
          AND (:date IS NULL OR d.date = :date)
        ORDER BY e.startDate ASC
    """)
    Page<Event> filterEvents(
            @Param("name") String name,
            @Param("date") LocalDate date,
            @Param("status") EventStatusEnum status,
            Pageable pageable
    );


    // ============================================================
    // 🔥 UPCOMING / ACTIVE / SETTLED (MULTI-DAY)
    // ============================================================
    // Sắp diễn ra: startDate > hôm nay
    @Query("""
        SELECT e FROM Event e
        WHERE e.startDate > :today
        ORDER BY e.startDate ASC
    """)
    List<Event> findUpcomingEvents(@Param("today") LocalDate today);

    // Đang diễn ra: today giữa startDate - endDate + status = ONGOING
    @Query("""
        SELECT e FROM Event e
        WHERE :today BETWEEN e.startDate AND e.endDate
          AND e.status = com.example.uniclub.enums.EventStatusEnum.ONGOING
        ORDER BY e.startDate ASC
    """)
    List<Event> findActiveEvents(@Param("today") LocalDate today);

    // Đã kết toán (COMPLETED) → sort theo endDate
    @Query("""
        SELECT e FROM Event e
        WHERE e.status = com.example.uniclub.enums.EventStatusEnum.COMPLETED
        ORDER BY e.endDate DESC
    """)
    List<Event> findAllSettledEvents();


    // ============================================================
    // 🔥 THỐNG KÊ THEO CLB + KHOẢNG THỜI GIAN
    // ============================================================
    @Query("""
        SELECT COUNT(e) FROM Event e
        WHERE e.hostClub.clubId = :clubId
          AND e.status = :status
          AND e.endDate BETWEEN :start AND :end
    """)
    int countCompletedInRange(
            @Param("clubId") Long clubId,
            @Param("status") EventStatusEnum status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT e FROM Event e
        WHERE e.hostClub.clubId = :clubId
          AND e.status = :status
          AND e.endDate BETWEEN :start AND :end
        ORDER BY e.endDate DESC
    """)
    List<Event> findCompletedEventsOfClubInRange(
            @Param("clubId") Long clubId,
            @Param("status") EventStatusEnum status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );


    // ============================================================
    // 🔥 PARTICIPATION (HOST + CO-HOST)
    // ============================================================
    @Query("""
        SELECT DISTINCT e FROM Event e
        LEFT JOIN e.coHostRelations r
        WHERE e.hostClub.clubId = :clubId
           OR r.club.clubId = :clubId
    """)
    List<Event> findByClubParticipation(@Param("clubId") Long clubId);

    @Query("""
        SELECT DISTINCT e FROM Event e
        JOIN e.coHostRelations rel
        WHERE rel.club.clubId = :clubId
          AND e.hostClub.clubId <> :clubId
    """)
    List<Event> findCoHostedEvents(@Param("clubId") Long clubId);


    // ============================================================
    // 🔥 EVENT + CO-HOST RELATIONS (DÙNG CHO DETAIL)
    // ============================================================
    @Query("""
        SELECT e FROM Event e
        LEFT JOIN FETCH e.coHostRelations rel
        LEFT JOIN FETCH rel.club
        WHERE e.eventId = :eventId
    """)
    Optional<Event> findByIdWithCoHostRelations(@Param("eventId") Long eventId);


    // ============================================================
    // 🔥 "MY EVENTS" (EVENTS CỦA USER)
    // ============================================================
    @Query("SELECT r.event FROM EventRegistration r WHERE r.user.userId = :userId")
    List<Event> findEventsByUserId(@Param("userId") Long userId);


    // ============================================================
    // 🔥 NHẮC HẠN ĐĂNG KÝ
    // ============================================================
    @Query("""
        SELECT e FROM Event e
        WHERE e.status = 'APPROVED'
          AND e.registrationDeadline BETWEEN :now AND :threshold
    """)
    List<Event> findEventsWithUpcomingRegistrationDeadline(
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold
    );

    @Query("""
        SELECT e FROM Event e
        JOIN e.days d
        WHERE e.location.locationId = :locationId
          AND d.date = :date
          AND d.startTime < :endTime
          AND d.endTime > :startTime
    """)
    List<Event> findConflictedEvents(
            @Param("locationId") Long locationId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
    @Query("""
    SELECT DISTINCT e FROM Event e
    JOIN e.days d
    WHERE d.date = :date
    ORDER BY e.startDate ASC
""")
    List<Event> findEventsByDate(@Param("date") LocalDate date);
    @Query("""
    SELECT COALESCE(SUM(e.currentCheckInCount), 0)
    FROM Event e
    LEFT JOIN e.coHostRelations r
    WHERE e.hostClub.clubId = :clubId
       OR r.club.clubId = :clubId
""")
    Long sumTotalCheckinByClub(@Param("clubId") Long clubId);


    @Query("""
    SELECT AVG(
        CASE 
            WHEN e.maxCheckInCount IS NULL OR e.maxCheckInCount = 0 
                THEN NULL
            ELSE (e.currentCheckInCount * 1.0 / e.maxCheckInCount)
        END
    )
    FROM Event e
    LEFT JOIN e.coHostRelations r
    WHERE e.hostClub.clubId = :clubId
       OR r.club.clubId = :clubId
""")
    Double avgCheckinRateByClub(@Param("clubId") Long clubId);
    @Query("""
    SELECT e FROM Event e
    WHERE e.hostClub.clubId = :clubId
      AND e.status = com.example.uniclub.enums.EventStatusEnum.COMPLETED
      AND e.endDate BETWEEN :start AND :end
    ORDER BY e.endDate DESC
""")
    List<Event> findCompletedEventsForClub(Long clubId, LocalDate start, LocalDate end);



    @Query("""
    SELECT DISTINCT e FROM Event e
    JOIN e.days d
    WHERE 
        d.date = :date
        AND d.startTime BETWEEN :startTime AND :endTime
        AND e.status = com.example.uniclub.enums.EventStatusEnum.APPROVED
""")
    List<Event> findEventsStartingSoon(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

}
