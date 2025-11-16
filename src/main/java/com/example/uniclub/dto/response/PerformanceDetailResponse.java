package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerformanceDetailResponse {
    private double baseScore;
    private double multiplier;
    private double finalScore;
}
