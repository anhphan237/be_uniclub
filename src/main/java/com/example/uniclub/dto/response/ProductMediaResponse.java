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
}
