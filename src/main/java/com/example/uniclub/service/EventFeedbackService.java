package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventFeedbackRequest;
import com.example.uniclub.dto.response.EventFeedbackResponse;
import com.example.uniclub.entity.User;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

public interface EventFeedbackService {

    List<EventFeedbackResponse> getFeedbacksByEvent(Long eventId);
    EventFeedbackResponse updateFeedback(Long feedbackId, EventFeedbackRequest req);
    void deleteFeedback(Long feedbackId);
    List<EventFeedbackResponse> getFeedbacksByMembership(Long membershipId);
    Map<String, Object> getFeedbackSummaryByEvent(Long eventId);
    List<EventFeedbackResponse> getFeedbacksByClub(Long clubId);
    EventFeedbackResponse createFeedback(Long eventId, EventFeedbackRequest req, CustomUserDetails userDetails);


}
