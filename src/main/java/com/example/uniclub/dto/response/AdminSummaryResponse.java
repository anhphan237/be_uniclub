package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSummaryResponse {
    private long totalUsers;
    private long totalClubs;
    private long totalEvents;
    private long totalRedeems;
    private long totalTransactions;
}