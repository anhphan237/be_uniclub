package com.example.uniclub.repository;

import com.example.uniclub.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface
AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByUser_UserIdAndEvent_EventId(Long userId, Long eventId);
}
