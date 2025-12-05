package com.example.uniclub.dto.response;

import com.example.uniclub.enums.RegistrationStatusEnum;
import java.time.LocalDateTime;

public record EventRegisteredUserResponse(
        Long userId,
        String fullName,
        String email,
        RegistrationStatusEnum status,
        LocalDateTime registeredAt,
        Integer committedPoints
) {}
