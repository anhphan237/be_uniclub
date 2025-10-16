package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MemberApplicationResponse {
    private Long applicationId;
    private Long clubId;
    private String clubName;
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private String status;
    private String motivation;
    private String attachmentUrl;
    private String note;
    private Long handledById;
    private String handledByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String studentCode;
    private String majorName;
    private String bio;
}
