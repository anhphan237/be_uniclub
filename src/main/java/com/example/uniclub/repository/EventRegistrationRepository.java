package com.example.uniclub.repository;

import com.example.uniclub.entity.EventRegistration;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.AttendanceLevelEnum;
import com.example.uniclub.enums.EventRegistrationStatusEnum;
import com.example.uniclub.enums.RegistrationStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    // 🔹 Kiểm tra xem user đã đăng ký event này chưa
    boolean existsByEvent_EventIdAndUser_UserId(Long eventId, Long userId);

    // 🔹 Lấy đăng ký theo event & user
    Optional<EventRegistration> findByEvent_EventIdAndUser_UserId(Long eventId, Long userId);

    // 🔹 Lấy danh sách đăng ký theo trạng thái
    List<EventRegistration> findByEvent_EventIdAndStatus(Long eventId, RegistrationStatusEnum status);

    // 🔹 Lấy danh sách đăng ký theo event
    List<EventRegistration> findByEvent_EventId(Long eventId);

    // 🔹 Lấy lịch sử đăng ký của user (sắp xếp mới nhất trước)
    List<EventRegistration> findByUser_UserIdOrderByRegisteredAtDesc(Long userId);

    // ✅ Đếm số event mà user đã đăng ký trong 1 khoảng thời gian (dùng cho MemberLevelScheduler)
    @Query("SELECT COUNT(r) FROM EventRegistration r WHERE r.user.userId = :userId AND r.registeredAt >= :since")
    long countByUser_UserIdAndRegisteredAtAfter(@Param("userId") Long userId, @Param("since") LocalDate since);

    @Query("SELECT COUNT(r) FROM EventRegistration r " +
            "WHERE r.user.userId = :userId " +
            "AND r.registeredAt BETWEEN :start AND :end")
    long countByUser_UserIdAndRegisteredAtBetween(
            @Param("userId") Long userId,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT e.event.id) FROM EventRegistration e WHERE e.user.id = :userId")
    long countDistinctEventsByUserId(@Param("userId") Long userId);

    @Query("SELECT r.user FROM EventRegistration r WHERE r.event.eventId = :eventId")
    List<User> findUsersByEventId(@Param("eventId") Long eventId);
    int countByEvent_EventId(Long eventId);

    long countByUser_UserIdAndStatus(Long userId, RegistrationStatusEnum status);
    @Query("""
    SELECT r.user.userId FROM EventRegistration r
    WHERE r.event.eventId = :eventId
""")
    List<Long> findUserIdsByEventId(Long eventId);
    boolean existsByEvent_EventId(Long eventId);

    boolean existsByEvent_EventIdAndAttendanceLevelNot(Long eventId, AttendanceLevelEnum level);
    long countByEvent_EventIdAndStatus(
            Long eventId,
            RegistrationStatusEnum status
    );

}
