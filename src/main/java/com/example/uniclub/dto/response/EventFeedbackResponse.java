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
    private String memberName;
    private Long membershipId;
    private Integer rating;
    private String comment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventFeedbackResponse fromEntity(com.example.uniclub.entity.EventFeedback feedback) {
        if (feedback == null) return null;

        var event = feedback.getEvent();
        var membership = feedback.getMembership();
        var club = (event != null && event.getHostClub() != null) ? event.getHostClub() : null;

        return EventFeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .eventId(event != null ? event.getEventId() : null)
                .eventName(event != null ? event.getName() : null)
                .clubName(club != null ? club.getName() : null)
                .memberName(
                        membership != null && membership.getUser() != null
                                ? membership.getUser().getFullName()
                                : null
                )
                .membershipId(membership != null ? membership.getMembershipId() : null)
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }


}
