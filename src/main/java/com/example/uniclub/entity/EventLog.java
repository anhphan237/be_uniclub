package com.example.uniclub.entity;

import com.example.uniclub.enums.UserActionEnum ;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String userName;
    private Long eventId;
    private String eventName;
    @Enumerated(EnumType.STRING)
    private UserActionEnum action;

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
