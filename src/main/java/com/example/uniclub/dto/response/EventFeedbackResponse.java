package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFeedbackResponse {

    private Long feedbackId;
    private Long eventId;
    private String eventName;
    private String clubName;

    private Long membershipId;
    private Integer rating;
    private String comment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
