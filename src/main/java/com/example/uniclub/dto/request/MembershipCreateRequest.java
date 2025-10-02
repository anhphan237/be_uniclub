package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

public record MembershipCreateRequest(
        @NotNull Long userId,
        @NotNull Long clubId,
        @NotBlank String level
) {}
