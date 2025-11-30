package com.example.uniclub.repository;

import com.example.uniclub.entity.OrderActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderActionLogRepository extends JpaRepository<OrderActionLog, Long> {

    List<OrderActionLog> findByTargetUser_UserIdAndOrder_Product_Event_EventIdOrderByCreatedAtDesc(
            Long userId,
            Long eventId
    );

    // 🔹 Lấy log theo actor (staff/leader thực hiện hành động)
    List<OrderActionLog> findByActor_UserIdOrderByCreatedAtDesc(Long actorId);

    // 🔹 Lấy toàn bộ log
    List<OrderActionLog> findAllByOrderByCreatedAtDesc();
}


