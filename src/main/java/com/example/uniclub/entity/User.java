package com.example.uniclub.entity;

import com.example.uniclub.enums.UserStatusEnum;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // ✅ FIXED: fetch role eagerly to avoid LazyInitializationException during JWT authentication
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "background_url")
    private String backgroundUrl;

    @Column(name = "student_code", nullable = true, unique = true)
    private String studentCode; // MSSV

    // 🔗 Mối quan hệ với chuyên ngành (vẫn giữ LAZY vì không cần khi login)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = true)
    private Major major;

    @Column(name = "bio", length = 500)
    private String bio;

    // 🔗 Danh sách membership (user có thể tham nhiều CLB)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Membership> memberships = new ArrayList<>();

    @Column(name = "is_first_login")
    private boolean isFirstLogin = true;
}
