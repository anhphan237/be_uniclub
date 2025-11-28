package com.example.uniclub.dto.response;

public record EventRatingSummaryDTO(
        Long eventId,
        Double averageRating,
        Long totalFeedbacks
) {}
