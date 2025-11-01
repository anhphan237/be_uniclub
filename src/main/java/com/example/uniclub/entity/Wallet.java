package com.example.uniclub.entity;

import com.example.uniclub.enums.WalletOwnerTypeEnum;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;

    @Column(nullable = false)
    private Long balancePoints = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletOwnerTypeEnum ownerType; // CLUB / EVENT / USER

    // ======================= RELATIONS =======================


    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonBackReference
    private User user;

    // Ví thuộc về 1 club
    @OneToOne
    @JoinColumn(name = "club_id", unique = true)
    @JsonBackReference
    private Club club;

    // Ví thuộc về 1 event
    @OneToOne
    @JoinColumn(name = "event_id", unique = true)
    @JsonBackReference
    private Event event;

    // ======================= AUDIT FIELDS =======================
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean active = true;
    @PrePersist
    @PreUpdate
    private void validateOwnerConsistency() {
        int count = 0;
        if (user != null) count++;
        if (club != null) count++;
        if (event != null) count++;
        if (count != 1) {
            throw new IllegalStateException(
                    "Wallet must belong to exactly one owner (club, event, or user)"
            );
        }
    }
}
