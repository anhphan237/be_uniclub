package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ClubCompareResponse {

    private ClubMonthlyBreakdownResponse clubA;
    private ClubMonthlyBreakdownResponse clubB;
}
