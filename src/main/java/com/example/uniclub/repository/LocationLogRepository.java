package com.example.uniclub.repository;

import com.example.uniclub.dto.response.LocationLogResponse;
import com.example.uniclub.entity.Event;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
          AND (:startDate IS NULL OR d.date >= :startDate)
          AND (:endDate IS NULL OR d.date <= :endDate)
          AND (:startTime IS NULL OR d.startTime >= :startTime)
          AND (:endTime IS NULL OR d.endTime <= :endTime)
        ORDER BY d.date, d.startTime
        """,
            countQuery = """
        SELECT count(d)
        FROM Event e
        JOIN e.days d
        WHERE e.location.locationId = :locationId
          AND (:eventId IS NULL OR e.eventId = :eventId)
          AND (:startDate IS NULL OR d.date >= :startDate)
          AND (:endDate IS NULL OR d.date <= :endDate)
          AND (:startTime IS NULL OR d.startTime >= :startTime)
          AND (:endTime IS NULL OR d.endTime <= :endTime)
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
}
