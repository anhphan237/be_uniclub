package com.example.uniclub.entity;

import com.example.uniclub.enums.*;
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

    private String clubName;
    private String description;
    private String category;
    private String proposerReason;

    @Enumerated(EnumType.STRING)
    private ApplicationSourceTypeEnum sourceType;

    @Enumerated(EnumType.STRING)
    private ClubApplicationStatusEnum status;

    @ManyToOne
    @JoinColumn(name = "proposer_id")
    private User proposer;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @ManyToOne
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    private Long majorId;

    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // ✅ THÊM MỚI
    @Column(length = 500)
    private String internalNote;   // Ghi chú nội bộ cho staff

    @Column(length = 500)
    private String attachmentUrl;  // Link file minh chứng (logo, giấy phép,...)

    @Enumerated(EnumType.STRING)
    @Column(name = "club_type")
    private ClubTypeEnum clubType;

}
