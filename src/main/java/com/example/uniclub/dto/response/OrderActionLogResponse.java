package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderActionLogResponse {

    private Long id;

    private String action;

    private Long actorId;
    private String actorName;

    private Long targetUserId;
    private String targetUserName;

    private Long orderId;

    private int quantity;
    private long pointsChange;
    private String reason;

    private LocalDateTime createdAt;
}
