package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "major_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MajorPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "major_id", nullable = false)
    private Long majorId;

    @Column(name = "major_name", nullable = false)
    private String majorName;

    @Column(name = "max_club_join", nullable = false)
    private Integer maxClubJoin;


    @Column(nullable = false)
    private boolean active;


}
