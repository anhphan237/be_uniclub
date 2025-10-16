package com.example.uniclub.dto.response;

import com.example.uniclub.enums.ClubApplicationStatusEnum;
import com.example.uniclub.enums.ApplicationSourceTypeEnum;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
public class ClubApplicationResponse {
    private Long applicationId;
    private String clubName;
    private String description;
    private String category;

    private SimpleUser submittedBy; // Người nộp đơn (student)
    private SimpleUser reviewedBy;  // Người duyệt (staff)

    private ClubApplicationStatusEnum status;
    private ApplicationSourceTypeEnum sourceType;
    private String rejectReason;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    @Data
    @Builder
    public static class SimpleUser {
        private String fullName;
        private String email;
    }
}
