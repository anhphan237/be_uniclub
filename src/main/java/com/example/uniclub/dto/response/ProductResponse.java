package com.example.uniclub.dto.response;

import java.util.List;

public record ProductResponse(
        Long productId,
        String name,
        String description,
        Integer pointCost,
        Integer stockQuantity,
        String type,
        Long clubId,
        Long eventId,
        Boolean isActive,
        List<MediaItem> media
) {
    public record MediaItem(Long mediaId, String url, String type, boolean isThumbnail, int displayOrder) {}
}
