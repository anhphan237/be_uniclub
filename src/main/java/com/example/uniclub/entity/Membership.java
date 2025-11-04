package com.example.uniclub.entity;

import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.MemberLevelEnum;
import com.example.uniclub.enums.MembershipStateEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    // 👤 Người dùng (User) thuộc membership này
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user;

    // 🏫 CLB mà user tham gia
    @ManyToOne(optional = false)
    @JoinColumn(name = "club_id")
    private Club club;

    // 🎭 Vai trò trong CLB
    @Enumerated(EnumType.STRING)
    @Column(name = "club_role", nullable = false, length = 30)
    private ClubRoleEnum clubRole = ClubRoleEnum.MEMBER;

    // 📌 Trạng thái tham gia
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStateEnum state = MembershipStateEnum.PENDING;

    // 👥 Là staff event hay không
    @Column(nullable = false)
    private boolean staff = false;

    // 📅 Ngày tham gia / rời CLB
    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberLevelEnum memberLevel = MemberLevelEnum.BASIC;

    @Column(nullable = false)
    private Double memberMultiplier = 1.0;
    // 🔹 Dữ liệu phụ trợ (không lưu DB)
    @Transient private String email;

    @Transient private String avatarUrl;

    @Transient private String studentCode;

    @Transient private String major;
}
