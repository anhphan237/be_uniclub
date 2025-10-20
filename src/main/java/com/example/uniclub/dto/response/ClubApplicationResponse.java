package com.example.uniclub.dto.response;

import com.example.uniclub.entity.ClubApplication;
import com.example.uniclub.enums.ClubApplicationStatusEnum;
import lombok.*;
import java.time.LocalDateTime;

/**
 * DTO phản hồi cho đơn xin tạo CLB (Student + UniStaff)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubApplicationResponse {

    private Long applicationId;
    private String clubName;
    private String description;
    private String majorName;
    private String vision;
    private String proposerReason;
    private SimpleUser proposer;
    private SimpleUser reviewedBy;
    private ClubApplicationStatusEnum status;
    private String rejectReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    // === Nested DTO cho người nộp & người duyệt ===
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleUser {
        private String fullName;
        private String email;
    }

    // ============================================================
    // ✅ Static mapper từ Entity → Response (tự viết tay)
    // ============================================================
    public static ClubApplicationResponse fromEntity(ClubApplication app) {
        if (app == null) return null;

        return ClubApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .clubName(app.getClubName())
                .description(app.getDescription())
                .majorName(app.getMajor().getName())
                .vision(app.getVision())
                .proposerReason(app.getProposerReason())
                .proposer(app.getProposer() == null ? null :
                        SimpleUser.builder()
                                .fullName(app.getProposer().getFullName())
                                .email(app.getProposer().getEmail())
                                .build())
                .reviewedBy(app.getReviewedBy() == null ? null :
                        SimpleUser.builder()
                                .fullName(app.getReviewedBy().getFullName())
                                .email(app.getReviewedBy().getEmail())
                                .build())
                .status(app.getStatus())
                .rejectReason(app.getRejectReason())
                .submittedAt(app.getCreatedAt())
                .reviewedAt(app.getReviewedAt())
                .build();
    }
}
