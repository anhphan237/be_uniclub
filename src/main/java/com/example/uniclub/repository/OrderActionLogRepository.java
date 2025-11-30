package com.example.uniclub.repository;

import com.example.uniclub.entity.OrderActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderActionLogRepository extends JpaRepository<OrderActionLog, Long> {

    @Query("""
        SELECT l FROM OrderActionLog l
        JOIN l.order o
        JOIN l.targetUser u
        WHERE o.orderId = :orderId
          AND u.userId = :userId
        ORDER BY l.createdAt DESC
    """)
    List<OrderActionLog> findByOrderIdAndUserId(@Param("orderId") Long orderId,
                                                @Param("userId") Long userId);

    @Query("""
        SELECT l FROM OrderActionLog l
        JOIN l.order o
        JOIN l.targetMember m
        WHERE o.orderId = :orderId
          AND m.membershipId = :membershipId
        ORDER BY l.createdAt DESC
    """)
    List<OrderActionLog> findByOrderIdAndMembershipId(
            @Param("orderId") Long orderId,
            @Param("membershipId") Long membershipId
    );

    // 🔹 Lấy log theo actor (staff/leader thực hiện hành động)
    List<OrderActionLog> findByActor_UserIdOrderByCreatedAtDesc(Long actorId);

    // 🔹 Lấy toàn bộ log
    List<OrderActionLog> findAllByOrderByCreatedAtDesc();
}


