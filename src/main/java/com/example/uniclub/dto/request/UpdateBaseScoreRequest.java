package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class UpdateBaseScoreRequest {
    private Long membershipId;
    private int baseScore;
}