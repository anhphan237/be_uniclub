package com.example.uniclub.entity;

import com.example.uniclub.enums.NotificationStatusEnum;
import com.example.uniclub.enums.NotificationTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTypeEnum type = NotificationTypeEnum.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatusEnum status = NotificationStatusEnum.UNREAD;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
