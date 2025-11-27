package com.example.uniclub.dto.response;

public record ReturnImageResponse(
        Long id,
        String imageUrl,
        String publicId,
        Integer displayOrder
) {}
