package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "club_attendance_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubAttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(nullable = false)
    private LocalDate date; // Ngày sinh hoạt (vd: 2025-10-29)

    private LocalTime startTime;   // 🕒 Giờ bắt đầu
    private LocalTime endTime;     // 🕕 Giờ kết thúc

    @Column(length = 255)
    private String note;           // Ghi chú (ví dụ: Buổi họp thường kỳ tháng 10)

    @Column(nullable = false)
    private boolean isLocked = false; // 🔒 Khóa qua ngày

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
