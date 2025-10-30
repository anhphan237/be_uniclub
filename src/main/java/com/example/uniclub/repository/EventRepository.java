package com.example.uniclub.repository;

import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByNameContainingIgnoreCaseAndDateAndStatus(
            String name, LocalDate date, EventStatusEnum status, Pageable pageable
    );

    Page<Event> findByNameContainingIgnoreCaseAndStatus(
            String name, EventStatusEnum status, Pageable pageable
    );

    Page<Event> findByNameContainingIgnoreCase(
            String name, Pageable pageable
    );

    Optional<Event> findByCheckInCode(String checkInCode);

    // ✅ Sửa lại theo tên field mới "hostClub"
    List<Event> findByHostClub_ClubId(Long clubId);

    List<Event> findByDateAfter(LocalDate date);

    @Query("SELECT r.event FROM EventRegistration r WHERE r.user.userId = :userId")
    List<Event> findEventsByUserId(@Param("userId") Long userId);

    long countByStatus(EventStatusEnum status);

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

    @Query("""
    SELECT e FROM Event e
    WHERE e.status = :status AND e.date >= :date
    ORDER BY e.date ASC
""")
    List<Event> findActiveEvents(@Param("status") EventStatusEnum status, @Param("date") LocalDate date);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.coHostRelations r " +
            "LEFT JOIN FETCH r.club " +
            "WHERE e.eventId = :eventId")
    Optional<Event> findByIdWithCoHostRelations(Long eventId);

    List<Event> findAllByStatusAndDate(EventStatusEnum status, LocalDate date);

}
