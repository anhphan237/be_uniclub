package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "majors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true, length = 20)
    private String majorCode; // 🆕 Mã ngành: SE, MKT, BA

    @Column(nullable = false)
    private boolean active = true;


    @OneToOne(mappedBy = "major", fetch = FetchType.LAZY)
    private MajorPolicy majorPolicy;

}
