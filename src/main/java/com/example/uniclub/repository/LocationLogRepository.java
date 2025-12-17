package com.example.uniclub.repository;

import com.example.uniclub.dto.response.LocationLogResponse;
import com.example.uniclub.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface LocationLogRepository extends JpaRepository<Event, Long> {

    @Query(value = """
        SELECT new com.example.uniclub.dto.response.LocationLogResponse(
            e.eventId, e.name, d.date, d.startTime, d.endTime
        )
        FROM Event e
        JOIN e.days d
        WHERE e.location.locationId = :locationId
          AND (:eventId IS NULL OR e.eventId = :eventId)
          AND d.date >= :startDate
          AND d.date <= :endDate
          AND d.startTime >= :startTime
          AND d.endTime <= :endTime
        ORDER BY d.date, d.startTime
        """,
            countQuery = """
        SELECT count(d)
        FROM Event e
        JOIN e.days d
        WHERE e.location.locationId = :locationId
          AND (:eventId IS NULL OR e.eventId = :eventId)
          AND d.date >= :startDate
          AND d.date <= :endDate
          AND d.startTime >= :startTime
          AND d.endTime <= :endTime
        """
    )
    Page<LocationLogResponse> findLogs(
            @Param("locationId") Long locationId,
            @Param("eventId") Long eventId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            Pageable pageable
    );

    @Query("""
        select new com.example.uniclub.dto.response.LocationLogResponse(
            h.eventId, h.eventName, h.date, h.startTime, h.endTime
        )
        from LocationEventHistory h
        where h.locationId = :locationId
          and (:eventId is null or h.eventId = :eventId)
          and h.date >= :startDate
          and h.date <= :endDate
          and h.startTime >= :startTime
          and h.endTime <= :endTime
        order by h.date, h.startTime
    """)
    List<LocationLogResponse> findFlatLogs(
            Long locationId,
            Long eventId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    );

}
