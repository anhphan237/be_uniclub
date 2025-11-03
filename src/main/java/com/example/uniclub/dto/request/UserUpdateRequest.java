package com.example.uniclub.dto.request;

import lombok.Builder;

@Builder
public record UserUpdateRequest(
        String fullName,
        String phone,
        Long majorId,
        String bio
) {}
