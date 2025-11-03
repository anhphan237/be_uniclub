package com.example.uniclub.repository;

import com.example.uniclub.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
    SELECT COUNT(DISTINCT DATE(ar.startCheckInTime))
    FROM AttendanceRecord ar
    WHERE ar.user.id = :userId
      AND ar.attendanceLevel <> 'NONE'
""")
    long countAttendanceDaysByUserId(@Param("userId") Long userId);


}
