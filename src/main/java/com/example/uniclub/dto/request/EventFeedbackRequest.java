package com.example.uniclub.dto.request;


import lombok.Data;


@Data
public class EventFeedbackRequest {
    private Integer rating;
    private String comment;
}


