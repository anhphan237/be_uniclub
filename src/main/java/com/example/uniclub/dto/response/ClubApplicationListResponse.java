package com.example.uniclub.dto.response;

import com.example.uniclub.enums.ClubApplicationStatusEnum;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Dùng cho LIST API
 * Không chứa LOB (description, vision, proposerReason...)
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubApplicationListResponse {

    private Long applicationId;
    private String clubName;

    private Long majorId;
    private String majorName;

    private ClubApplicationStatusEnum status;
    private LocalDateTime submittedAt;
}
