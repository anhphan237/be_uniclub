package com.example.uniclub.dto.request;

import com.example.uniclub.enums.MemberActivityLevelEnum;
import jakarta.validation.constraints.NotNull;

public record MembershipPromoteRequest(
        @NotNull MemberActivityLevelEnum newLevel
) {}

