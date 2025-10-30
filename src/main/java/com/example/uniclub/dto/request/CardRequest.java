package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class CardRequest {
    private String borderRadius;
    private String cardColorClass;
    private Integer cardOpacity;
    private String colorType;
    private String gradient;
    private Integer logoSize;
    private String pattern;
    private Integer patternOpacity;
    private String qrPosition;
    private Integer qrSize;
    private String qrStyle;
    private Boolean showLogo;
    private String logoUrl;
}
