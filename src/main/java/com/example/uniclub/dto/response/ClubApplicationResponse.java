package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClubApplicationResponse {
    private Long applicationId;
    private String clubName;
    private String description;

    private SimpleUser submittedBy;
    private SimpleUser reviewedBy;

    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    @Data
    @Builder
    public static class SimpleUser {
        private String fullName;
        private String email;
    }
}