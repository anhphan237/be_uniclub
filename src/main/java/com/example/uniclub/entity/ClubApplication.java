package com.example.uniclub.entity;

import com.example.uniclub.enums.ApplicationStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_name", "status"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @Column(nullable = false)
    private String clubName;  // tên CLB xin mở

    private String description; // mô tả CLB

    @ManyToOne(optional = false)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;  // người nộp đơn (Club Manager)

    @Enumerated(EnumType.STRING)
    private ApplicationStatusEnum status;

    private LocalDateTime submittedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;
}

