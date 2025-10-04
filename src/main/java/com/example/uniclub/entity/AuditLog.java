package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String action;      // e.g., CREATE_EVENT, APPROVE_MEMBER
    private String entityType;  // e.g., "Event","Membership"
    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
