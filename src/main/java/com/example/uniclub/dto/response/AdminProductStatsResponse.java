package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminProductStatsResponse {
    private long total;
    private long active;
    private long inactive;
    private long archived;
}
