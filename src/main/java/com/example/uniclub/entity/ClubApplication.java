package com.example.uniclub.entity;

import com.example.uniclub.enums.ClubApplicationStatusEnum;
import com.example.uniclub.enums.ApplicationSourceTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "club_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @ManyToOne
    @JoinColumn(name = "proposer_id")
    private User proposer; // Người nộp đơn (chỉ có khi ONLINE)

    @Column(nullable = false, unique = true)
    private String clubName;

    private String description;
    private String category;
    private String proposerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubApplicationStatusEnum status = ClubApplicationStatusEnum.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationSourceTypeEnum sourceType = ApplicationSourceTypeEnum.ONLINE;

    private String rejectReason;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
}
