package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    // 🔹 Liên kết đến sự kiện (Event)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 🔹 Liên kết đến membership (người tham gia)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    // 🔹 Điểm đánh giá 1–5 ⭐
    @Column(nullable = false)
    private Integer rating;

    // 🔹 Bình luận
    @Column(columnDefinition = "TEXT")
    private String comment;

    // 🔹 Ngày tạo (tự động set khi insert)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 🔹 Ngày cập nhật (tự động set khi update)
    @Column
    private LocalDateTime updatedAt;

    // ✅ Tự động set thời gian khi insert vào DB
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ✅ Tự động set thời gian khi update
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
