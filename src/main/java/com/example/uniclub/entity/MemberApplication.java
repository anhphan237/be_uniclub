package com.example.uniclub.entity;

import com.example.uniclub.enums.MemberApplyStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_app_unique_active",
                columnNames = {"user_id", "club_id", "active_flag"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberApplyStatusEnum status = MemberApplyStatusEnum.PENDING;

    @Column(columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;

    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        submittedAt = LocalDateTime.now();
        updatedAt = submittedAt;
        if (status == null) status = MemberApplyStatusEnum.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
