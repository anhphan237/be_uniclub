package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_staffs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "membership_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Liên kết đến Event
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    // 🔹 Liên kết đến Membership (thành viên CLB)
    @ManyToOne(optional = false)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    // 🔹 Mô tả nhiệm vụ (ví dụ: checkin desk, logistics, photographer,...)
    @Column(length = 100)
    private String duty;
}
