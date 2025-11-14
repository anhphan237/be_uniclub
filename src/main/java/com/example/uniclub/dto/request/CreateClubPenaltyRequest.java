package com.example.uniclub.dto.request;

import com.example.uniclub.enums.ClubPenaltyTypeEnum;
import jakarta.validation.constraints.NotNull;

public record CreateClubPenaltyRequest(
        @NotNull Long membershipId,
        Long eventId,
        @NotNull ClubPenaltyTypeEnum type,
        Integer points,   // nếu null → dùng default mapping theo loại phạt
        String reason
) {}
