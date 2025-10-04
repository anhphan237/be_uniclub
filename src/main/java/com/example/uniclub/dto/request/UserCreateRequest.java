package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;

public record UserCreateRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String fullName,
        String phone,
        @NotNull Long roleId
) {}
