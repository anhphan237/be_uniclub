package com.example.uniclub.dto.response;

public record ClubRatingSummaryDTO(
        Long clubId,
        Long totalRating,
        Long totalFeedbacks,
        Double averageRating
) {}