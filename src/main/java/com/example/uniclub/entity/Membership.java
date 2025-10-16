package com.example.uniclub.entity;

import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Builder(toBuilder = true)
@Entity
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "club_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long membershipId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    // ✅ Vai trò hành chính trong CLB
    @Enumerated(EnumType.STRING)
    @Column(name = "club_role", nullable = false, length = 30)
    private ClubRoleEnum clubRole = ClubRoleEnum.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStateEnum state = MembershipStateEnum.PENDING;

    @Column(nullable = false)
    private boolean staff = false;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
