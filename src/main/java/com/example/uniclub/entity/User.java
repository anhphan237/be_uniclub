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


    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;

    // ✅ Thông tin hồ sơ mở rộng
    @Column(name = "student_code", nullable = false, unique = true)
    private String studentCode; // MSSV (duy nhất, không thể đổi)

    @Column(name = "major_name")
    private String majorName;   // Chuyên ngành

    @Column(name = "bio", length = 500)
    private String bio;         // Giới thiệu bản thân (optional)
}
