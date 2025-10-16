package com.example.uniclub.dto.request;

import com.example.uniclub.enums.MemberLevelEnum;
import jakarta.validation.constraints.NotNull;

public record MembershipPromoteRequest(
        @NotNull MemberLevelEnum newLevel
) {}
