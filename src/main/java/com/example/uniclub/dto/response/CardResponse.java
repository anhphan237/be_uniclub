package com.example.uniclub.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CardResponse {
    private Long cardId;
    private Long clubId;
    private String clubName;
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
    private LocalDateTime createdAt;
}
