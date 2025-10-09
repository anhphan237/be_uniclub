package com.example.uniclub.dto.response;

import com.example.uniclub.enums.MemberApplyStatusEnum;
import java.time.LocalDateTime;

public record MemberApplicationResponse(
        Long applicationId,
        Long userId,
        String userName,
        Long clubId,
        String clubName,
        MemberApplyStatusEnum status,
        String reason,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt
) {}
