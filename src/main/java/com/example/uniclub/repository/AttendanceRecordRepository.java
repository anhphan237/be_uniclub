package com.example.uniclub.repository;

import com.example.uniclub.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    // ⚙️ Dùng cho logic checkin (START / MID / END)
    Optional<AttendanceRecord> findByUser_UserIdAndEvent_EventId(Long userId, Long eventId);

    boolean existsByUser_UserIdAndEvent_EventId(Long userId, Long eventId);

    // ✅ Tổng attendance toàn trường
    @Query(value = "SELECT COUNT(*) FROM attendance_records", nativeQuery = true)
    long countTotalAttendances();

    // ✅ Số attendance từng CLB (group by)
    @Query(value = """
        SELECT e.host_club_id AS club_id, c.name AS club_name, COUNT(a.*) AS total_attendances
        FROM attendance_records a
        JOIN events e ON e.event_id = a.event_id
        JOIN clubs c ON c.club_id = e.host_club_id
        GROUP BY e.host_club_id, c.name
        ORDER BY total_attendances DESC
        """, nativeQuery = true)
    List<Object[]> getClubAttendanceRanking();

    // ✅ Biểu đồ tổng hợp tháng
    @Query(value = """
        SELECT 
            DATE_TRUNC('month', a.start_check_in_time) AS month,
            COUNT(DISTINCT a.user_id) AS participant_count
        FROM attendance_records a
        WHERE EXTRACT(YEAR FROM a.start_check_in_time) = :year
        GROUP BY month
        ORDER BY month
    """, nativeQuery = true)
    List<Object[]> getMonthlyAttendanceSummary(@Param("year") int year);

    @Query(value = """
        SELECT 
            DATE_TRUNC('month', a.start_check_in_time) AS month,
            COUNT(DISTINCT a.user_id) AS participant_count
        FROM attendance_records a
        JOIN events e ON e.event_id = a.event_id
        WHERE EXTRACT(YEAR FROM a.start_check_in_time) = :year
          AND e.host_club_id = :clubId
        GROUP BY month
        ORDER BY month
    """, nativeQuery = true)
    List<Object[]> getMonthlyAttendanceSummaryByClub(
            @Param("year") int year,
            @Param("clubId") Long clubId
    );

    @Query(value = """
        SELECT 
            DATE_TRUNC('month', a.start_check_in_time) AS month,
            COUNT(DISTINCT a.user_id) AS participant_count
        FROM attendance_records a
        WHERE EXTRACT(YEAR FROM a.start_check_in_time) = :year
          AND a.event_id = :eventId
        GROUP BY month
        ORDER BY month
    """, nativeQuery = true)
    List<Object[]> getMonthlyAttendanceSummaryByEvent(
            @Param("year") int year,
            @Param("eventId") Long eventId
    );

    @Query(value = """
        SELECT COUNT(DISTINCT DATE(start_check_in_time))
        FROM attendance_records
        WHERE user_id = :userId
          AND attendance_level <> 'NONE'
    """, nativeQuery = true)
    long countAttendanceDaysByUserId(@Param("userId") Long userId);

    // THÊM vào AttendanceRecordRepository
    @Query("""
    SELECT COUNT(ar) FROM AttendanceRecord ar
    WHERE ar.user.userId = :userId
      AND ar.event.eventId IN :eventIds
      AND ar.attendanceLevel IN :levels
""")
    long countByUserAndEventsAndLevels(
            @Param("userId") Long userId,
            @Param("eventIds") java.util.List<Long> eventIds,
            @Param("levels") java.util.List<com.example.uniclub.enums.AttendanceLevelEnum> levels
    );

    @Query("""
        SELECT ar FROM AttendanceRecord ar
        WHERE ar.user.userId = :userId
          AND ar.event.startDate <= :end
          AND ar.event.endDate >= :start
    """)
    List<AttendanceRecord> findByUserIdAndEventDateBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
    @Query("""
    SELECT ar
    FROM AttendanceRecord ar
    JOIN ar.event e
    LEFT JOIN e.coHostRelations r
    WHERE e.hostClub.clubId = :clubId
       OR r.club.clubId = :clubId
""")
    List<AttendanceRecord> findAttendanceByClub(@Param("clubId") Long clubId);
    @Query("""
    SELECT ar
    FROM AttendanceRecord ar
    JOIN ar.event e
    LEFT JOIN e.coHostRelations r
    WHERE (e.hostClub.clubId = :clubId OR r.club.clubId = :clubId)
      AND ar.startCheckInTime BETWEEN :start AND :end
""")
    List<AttendanceRecord> findAttendanceByClubAndDateRange(
            @Param("clubId") Long clubId,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );

    @Query("""
        SELECT ar
        FROM AttendanceRecord ar
        JOIN FETCH ar.event e
        WHERE ar.user.userId = :userId
          AND (
               ar.startCheckInTime IS NOT NULL
            OR ar.midCheckTime IS NOT NULL
            OR ar.endCheckOutTime IS NOT NULL
          )
          AND e.status IN (
               com.example.uniclub.enums.EventStatusEnum.ONGOING,
               com.example.uniclub.enums.EventStatusEnum.COMPLETED
          )
    """)
    List<AttendanceRecord> findMyCheckedInEvents(
            @Param("userId") Long userId
    );
    Optional<AttendanceRecord> findByEvent_EventIdAndUser_UserId(
            Long eventId,
            Long userId
    );
}
