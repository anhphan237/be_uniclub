package com.example.uniclub.dto.response;

import com.example.uniclub.enums.ApplicationSourceTypeEnum;
import com.example.uniclub.enums.ClubApplicationStatusEnum;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubApplicationResponse {

    private Long applicationId;
    private String clubName;
    private String description;
    private String category;
    private SimpleUser submittedBy;
    private SimpleUser reviewedBy;
    private ClubApplicationStatusEnum status;
    private ApplicationSourceTypeEnum sourceType;
    private String rejectReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    // ✅ Thêm mới để fix lỗi
    private String attachmentUrl;  // file minh chứng (upload)
    private String internalNote;   // ghi chú nội bộ staff/admin

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
}
