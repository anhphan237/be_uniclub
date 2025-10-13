package com.example.uniclub.entity;

import com.example.uniclub.enums.UserStatusEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;
    private String phone;

    @Column(nullable = false)
    private String status = UserStatusEnum.ACTIVE.name();

    // 🧩 Avatar URL (Google / Upload)
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;

    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode; // MSSV

    @Column(name = "major_name")
    private String majorName;

    @Column(name = "bio", length = 500)
    private String bio;
}
