package com.example.uniclub.dto.request;

public record PublicQrCheckInRequest(
        String checkInCode,
        String phase
) {}
