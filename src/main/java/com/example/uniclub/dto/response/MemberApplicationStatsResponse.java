package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberApplicationStatsResponse {
    private Long clubId;
    private String clubName;
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private String date;
    private long count;
}
