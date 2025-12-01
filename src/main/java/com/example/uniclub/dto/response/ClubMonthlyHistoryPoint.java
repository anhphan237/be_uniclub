package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ClubMonthlyHistoryPoint {

    private int month;
    private double score;
}
