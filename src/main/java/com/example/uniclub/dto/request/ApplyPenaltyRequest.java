package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class ApplyPenaltyRequest {
    private Long membershipId;
    private Long ruleId;
    private Long eventId; // optional
    private String note;  // optional
}