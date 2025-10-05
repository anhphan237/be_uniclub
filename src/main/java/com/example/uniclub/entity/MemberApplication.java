package com.example.uniclub.entity;

import com.example.uniclub.enums.ApplicationStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","club_id","status"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @Enumerated(EnumType.STRING)
    private ApplicationStatusEnum status = ApplicationStatusEnum.SUBMITTED;

    private LocalDateTime submittedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
}
