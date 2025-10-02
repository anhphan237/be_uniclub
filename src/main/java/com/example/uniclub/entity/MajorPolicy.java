package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "major_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MajorPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long majorPolicyId;

    @Column(nullable = false, unique = true)
    private String name;
}
