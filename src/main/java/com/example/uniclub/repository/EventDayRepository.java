package com.example.uniclub.repository;

import com.example.uniclub.entity.EventDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventDayRepository extends JpaRepository<EventDay, Long> {

    // Lấy danh sách ngày của 1 event
    List<EventDay> findByEvent_EventId(Long eventId);

    // Lấy ngày cụ thể theo event + date
    Optional<EventDay> findByEvent_EventIdAndDate(Long eventId, LocalDate date);

    // Lấy ngày đầu tiên của event
    Optional<EventDay> findFirstByEvent_EventIdOrderByDateAsc(Long eventId);

    // Lấy ngày cuối cùng của event
    Optional<EventDay> findFirstByEvent_EventIdOrderByDateDesc(Long eventId);

    // Kiểm tra event có ngày trùng
    boolean existsByEvent_EventIdAndDate(Long eventId, LocalDate date);

    @Query("""
        SELECT DISTINCT d.event.eventId
        FROM EventDay d
        WHERE d.event.hostClub.clubId = :clubId
          AND d.date BETWEEN :start AND :end
    """)
    List<Long> findEventIdsByClubAndMonth(
            @Param("clubId") Long clubId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

}