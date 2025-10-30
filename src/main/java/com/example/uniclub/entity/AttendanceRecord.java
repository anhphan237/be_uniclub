package com.example.uniclub.entity;

import com.example.uniclub.enums.AttendanceLevelEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendanceId;

    // 🔹 Liên kết user (student)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔹 Liên kết event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 🔹 3 pha check-in
    private LocalDateTime startCheckInTime;
    private LocalDateTime midCheckTime;
    private LocalDateTime endCheckOutTime;

    // 🔹 Mức độ tham dự (NONE / HALF / FULL)
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_level")
    private AttendanceLevelEnum attendanceLevel;
}
