package com.example.uniclub.dto.response;

import com.example.uniclub.entity.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class MemberRewardMonthlyResponse {
    private int year;
    private int month;
    private long totalReward;
    private List<WalletTransaction> transactions;
}
