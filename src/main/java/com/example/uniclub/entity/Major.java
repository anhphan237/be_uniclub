package com.example.uniclub.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    private String majorCode; // VD: SE, MKT, AI

    @Column(nullable = false)
    private boolean active = true;
    @Column(length = 10)
    private String colorHex;

    // 🔁 Một Major có thể có nhiều Policy
    @OneToMany(mappedBy = "major", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference   // ✅ thêm dòng này
    private List<MajorPolicy> policies = new ArrayList<>();
}


