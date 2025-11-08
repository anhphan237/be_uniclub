package com.example.uniclub.entity;

import com.example.uniclub.enums.LeaveRequestStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubLeaveRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    private Membership membership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveRequestStatusEnum status = LeaveRequestStatusEnum.PENDING;

    @Column(length = 500)
    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
