//package com.example.uniclub.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "topups")
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class Topup {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long topupId;
//
//    @ManyToOne(optional = false)
//    @JoinColumn(name = "wallet_id")
//    private Wallet wallet;
//
//    // Số tiền VNĐ (ví dụ: 10_000 VND)
//    @Column(nullable = false)
//    private Long amountMoney;
//
//    // Điểm quy đổi (ví dụ: 10_000 VND => 100 points)
//    @Column(nullable = false)
//    private Integer convertedPoints;
//
//    // Ai khởi tạo giao dịch nạp (admin/manager/user)
//    @ManyToOne
//    @JoinColumn(name = "initiated_by")
//    private User initiatedBy;
//
//    @Column(nullable = false)
//    private LocalDateTime createdAt = LocalDateTime.now();
//}
