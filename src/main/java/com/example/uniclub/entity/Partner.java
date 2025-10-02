package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partners")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long partnerId;

    @Column(nullable = false, unique = true)
    private String name;

    private String contactInfo;
}
