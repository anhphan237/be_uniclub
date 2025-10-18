package com.example.uniclub.repository;

import com.example.uniclub.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    boolean existsByEventIdAndStudentId(Long eventId, Long studentId);
}
