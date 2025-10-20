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

    // Thông tin CLB
    private String clubName;
    private String description;
    private String major;      // chuyên ngành CLB
    private String vision;     // tầm nhìn CLB
    private String proposerReason;


    @Enumerated(EnumType.STRING)
    private ClubApplicationStatusEnum status;

    // Người nộp đơn
    @ManyToOne
    @JoinColumn(name = "proposer_id", nullable = false)
    private User proposer;
    @ManyToOne
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;

    // Người duyệt đơn
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // Gắn với CLB sau khi khởi tạo chính thức
    @OneToOne
    @JoinColumn(name = "club_id")
    private Club club;
}
