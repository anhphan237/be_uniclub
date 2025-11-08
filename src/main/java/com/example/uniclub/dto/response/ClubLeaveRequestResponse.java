package com.example.uniclub.dto.response;

import com.example.uniclub.enums.LeaveRequestStatusEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClubLeaveRequestResponse {
    private Long requestId;

    private Long membershipId;
    private String memberName;
    private String memberEmail;
    private String memberRole;

    private String reason;
    private LeaveRequestStatusEnum status;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
