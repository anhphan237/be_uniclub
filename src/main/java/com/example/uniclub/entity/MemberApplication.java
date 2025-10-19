package com.example.uniclub.entity;

import com.example.uniclub.enums.MemberApplicationStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_applications",
        indexes = {
                @Index(columnList = "status"),
                @Index(columnList = "createdAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applicationId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User applicant;


    @ManyToOne
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberApplicationStatusEnum status = MemberApplicationStatusEnum.PENDING;

    @Column(length = 1000)
    private String message;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
