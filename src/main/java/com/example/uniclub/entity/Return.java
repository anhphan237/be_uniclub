package com.example.uniclub.entity;

import com.example.uniclub.enums.ReturnStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Return {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_tx_id")
    private ProductTransaction productTx;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatusEnum status = ReturnStatusEnum.REQUESTED;

    private LocalDateTime processedAt;
}
