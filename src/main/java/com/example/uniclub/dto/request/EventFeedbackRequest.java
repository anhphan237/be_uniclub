package com.example.uniclub.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventFeedbackRequest {

    @NotNull
    private Long eventId;

    @NotNull
    private Long membershipId;

    @Min(1)
    @Max(5)
    private Integer rating;

    private String comment;
}
