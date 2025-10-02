package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","club_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    private String level;  // e.g., BASIC, SILVER, GOLD
    private String state;  // active, inactive, suspended

    private LocalDateTime joinedAt = LocalDateTime.now();
}
