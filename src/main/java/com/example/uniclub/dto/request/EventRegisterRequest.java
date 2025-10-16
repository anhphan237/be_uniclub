package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record EventRegisterRequest(
        @NotNull Long eventId
) {}
