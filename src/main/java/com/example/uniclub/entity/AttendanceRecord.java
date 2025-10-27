package com.example.uniclub.entity;

import com.example.uniclub.enums.AttendanceLevelEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private Long studentId;
    private LocalDateTime checkinTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_level")
    private AttendanceLevelEnum attendanceLevel;



    public AttendanceRecord(Long eventId, Long studentId) { this.eventId = eventId; this.studentId = studentId; this.checkinTime = LocalDateTime.now(); }
}
