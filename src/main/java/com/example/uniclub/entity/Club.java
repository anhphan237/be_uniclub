package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long clubId;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // ✅ Thêm quan hệ leader (trưởng CLB)
    @ManyToOne
    @JoinColumn(name = "leader_id")
    private User leader;

    @ManyToOne(optional = false)
    @JoinColumn(name = "major_policy_id")
    private MajorPolicy majorPolicy;

    @ManyToOne
    @JoinColumn(name = "major_id")
    private Major major;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToOne
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;
}
