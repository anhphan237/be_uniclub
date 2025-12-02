package com.example.uniclub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private String type;      // CLUB, EVENT, SYSTEM
    private String title;     // Short title
    private String message;   // Detailed explanation

    // =====================================================
    // STATIC HELPERS FOR CLEAN CODE
    // =====================================================
    public static RecommendationResponse buildClub(String title, String message) {
        return RecommendationResponse.builder()
                .type("CLUB")
                .title(title)
                .message(message)
                .build();
    }

    public static RecommendationResponse buildEvent(String title, String message) {
        return RecommendationResponse.builder()
                .type("EVENT")
                .title(title)
                .message(message)
                .build();
    }

    public static RecommendationResponse buildSystem(String title, String message) {
        return RecommendationResponse.builder()
                .type("SYSTEM")
                .title(title)
                .message(message)
                .build();
    }
}
