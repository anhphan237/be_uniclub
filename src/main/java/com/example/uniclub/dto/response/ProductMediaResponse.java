package com.example.uniclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMediaResponse {
    private Long mediaId;
    private String url;
    private String type;
    private boolean isThumbnail;
    private int displayOrder;
    public static ProductMediaResponse fromEntity(com.example.uniclub.entity.ProductMedia media) {
        if (media == null) return null;
        return new ProductMediaResponse(
                media.getMediaId(),
                media.getUrl(),
                media.getType(),
                media.isThumbnail(),
                media.getDisplayOrder()
        );
    }

}
