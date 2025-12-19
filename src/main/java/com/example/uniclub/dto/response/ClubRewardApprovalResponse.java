package com.example.uniclub.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubRewardApprovalResponse {

    private Long clubId;
    private String clubName;

    private int year;
    private int month;

    private long rewardPoints;

    private boolean locked;
    private LocalDateTime lockedAt;
    private String lockedBy;

    private long walletBalance;

    private boolean approved;
    private String approvedBy;
    private LocalDateTime approvedAt;
}

