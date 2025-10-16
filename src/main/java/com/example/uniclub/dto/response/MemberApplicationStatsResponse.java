package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberApplicationStatsResponse {
    private long pending;
    private long interviewing;
    private long approved;
    private long rejected;
    private long expired;
}
