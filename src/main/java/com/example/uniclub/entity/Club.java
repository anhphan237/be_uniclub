package com.example.uniclub.entity;

import com.example.uniclub.enums.ClubActivityStatusEnum;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    @Column(name = "vision", length = 500)
    private String vision;

    // 👤 User làm chủ nhiệm CLB
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    // 💰 Ví riêng của CLB (liên kết 1–1 với Wallet)
    @OneToOne(mappedBy = "club", cascade = CascadeType.MERGE)
    private Wallet wallet;

    @Builder.Default
    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 0;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubActivityStatusEnum activityStatus = ClubActivityStatusEnum.ACTIVE;

    @Column(nullable = false)
    private Double clubMultiplier = 1.0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Club)) return false;
        Club other = (Club) o;
        return clubId != null && clubId.equals(other.getClubId());
    }

    @Override
    public int hashCode() {
        return 31;
    }

}
