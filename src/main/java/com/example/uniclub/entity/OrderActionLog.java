package com.example.uniclub.entity;

import com.example.uniclub.enums.OrderActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_action_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private ProductOrder order;

    // Người bị ảnh hưởng (người mua/đổi)
    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne
    @JoinColumn(name = "target_member_id")
    private Membership targetMember;

    // Người thực hiện hành động (staff/leader/user)
    @ManyToOne
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    private OrderActionType action;

    private long pointsChange;
    private int quantity;
    private String reason;

    private LocalDateTime createdAt;
}
