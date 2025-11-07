package com.example.uniclub.repository;

import com.example.uniclub.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByEventIdOrderByCreatedAtDesc(Long eventId);
    List<EventLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
