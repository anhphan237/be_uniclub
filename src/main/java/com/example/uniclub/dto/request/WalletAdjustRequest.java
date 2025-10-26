package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletAdjustRequest(
        @NotNull @Min(1) Integer amount,
        String description
) {}
