package com.example.uniclub.dto.response;

import com.example.uniclub.enums.MemberApplyStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberApplicationResponse {
    private Long applicationId;
    private Long userId;
    private String userName;
    private Long clubId;
    private String clubName;
    private MemberApplyStatusEnum status;
    private String reason;
    private String reviewedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
