package com.example.uniclub.dto.response;

import com.example.uniclub.enums.MemberActivityLevelEnum;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRewardSuggestionResponse {

    private Long membershipId;
    private String fullName;
    private MemberActivityLevelEnum activityLevel;

    private double baseScore;
    private double finalScore;

    private int suggestedPoints;
    private String suggestionReason;
}
