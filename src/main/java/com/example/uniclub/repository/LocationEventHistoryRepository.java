package com.example.uniclub.repository;

import com.example.uniclub.dto.response.LocationLogResponse;
import com.example.uniclub.entity.LocationEventHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationEventHistoryRepository extends JpaRepository<LocationEventHistory, Long> {

    Optional<LocationEventHistory> findFirstByEventDayIdAndValidToIsNull(Long eventDayId);

    @Modifying
    @Query("""
        update LocationEventHistory h
           set h.validTo = :now
         where h.eventDayId = :eventDayId
           and h.validTo is null
    """)
    int closeActiveByEventDayId(@Param("eventDayId") Long eventDayId, @Param("now") Instant now);

    @Query(value = """
        select new com.example.uniclub.dto.response.LocationLogResponse(
            h.eventId, h.eventName, h.date, h.startTime, h.endTime
        )
        from LocationEventHistory h
        where h.locationId = :locationId
          and (:eventId is null or h.eventId = :eventId)
          and (:startDate is null or h.date >= :startDate)
          and (:endDate is null or h.date <= :endDate)
          and (:startTime is null or h.startTime >= :startTime)
          and (:endTime is null or h.endTime <= :endTime)
        order by h.date, h.startTime
    """)
    Page<LocationLogResponse> findHistory(
            @Param("locationId") Long locationId,
            @Param("eventId") Long eventId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            Pageable pageable
    );

    @Query(value = """
        select new com.example.uniclub.dto.response.LocationLogResponse(
            h.eventId, h.eventName, h.date, h.startTime, h.endTime
        )
        from LocationEventHistory h
        where h.locationId = :locationId
          and h.validTo is null
          and (:eventId is null or h.eventId = :eventId)
          and (:startDate is null or h.date >= :startDate)
          and (:endDate is null or h.date <= :endDate)
          and (:startTime is null or h.startTime >= :startTime)
          and (:endTime is null or h.endTime <= :endTime)
        order by h.date, h.startTime
    """)
    Page<LocationLogResponse> findCurrent(
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
