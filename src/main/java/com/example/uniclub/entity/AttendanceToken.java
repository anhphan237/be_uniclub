package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;
    private String token;
    private LocalDateTime expiredAt;
    private Boolean isUsed = false;
}
