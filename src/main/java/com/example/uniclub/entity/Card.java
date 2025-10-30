package com.example.uniclub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    // 🔗 Khóa ngoại tới Club
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // 🧩 Các thuộc tính thiết kế
    private String borderRadius;     // vd: "rounded-xl"
    private String cardColorClass;   // vd: "bg-gradient-to-r"
    private Integer cardOpacity;     // vd: 80
    private String colorType;        // vd: "pastel"
    private String gradient;         // vd: "from-orange-200 via-amber-200 to-yellow-200"
    private Integer logoSize;        // vd: 60
    private String pattern;          // vd: "diagonal"
    private Integer patternOpacity;  // vd: 20
    private String qrPosition;       // vd: "center-right"
    private Integer qrSize;          // vd: 100
    private String qrStyle;          // vd: "rounded"
    private Boolean showLogo;        // vd: true

    // 🖼️ Logo URL (Base64 hoặc Cloudinary link giống avatar)
    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    // 🕒 Ngày tạo
    @CreationTimestamp
    private LocalDateTime createdAt;
}
