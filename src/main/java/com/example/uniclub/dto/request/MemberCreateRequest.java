package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

public record MemberCreateRequest(
        @NotNull Long userId,
        @NotNull Long clubId,
        @NotBlank String level
) {}
