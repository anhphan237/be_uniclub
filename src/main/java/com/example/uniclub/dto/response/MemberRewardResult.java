package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberRewardResult {
    private Long membershipId;
    private Long userId;
    private String fullName;
    private String studentCode;

    private int baseFinalScore;
    private int rewardScore;
    private int totalFinalScore;
}

