package com.example.uniclub.repository;

import com.example.uniclub.entity.EventRegistration;
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
}
