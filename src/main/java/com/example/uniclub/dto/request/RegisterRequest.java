package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String fullName,
        @NotBlank String phone,
        @NotNull String roleName
) {}
