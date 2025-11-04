package com.example.uniclub.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFeedbackRequest {
    private Integer rating;
    private String comment;
}
