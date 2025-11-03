package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventFeedbackRequest;
import com.example.uniclub.dto.response.EventFeedbackResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.EventFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventFeedbackServiceImpl implements EventFeedbackService {

    private final EventFeedbackRepository feedbackRepo;
    private final EventRepository eventRepo;
    private final MembershipRepository membershipRepo;

    @Override
    public EventFeedbackResponse createFeedback(EventFeedbackRequest req) {
        Event event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        Membership membership = membershipRepo.findById(req.getMembershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        EventFeedback feedback = feedbackRepo.save(EventFeedback.builder()
                .event(event)
                .membership(membership)
                .rating(req.getRating())
                .comment(req.getComment())
                .build());

        return EventFeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .eventId(event.getEventId())
                .eventName(event.getName())
                .clubName(event.getHostClub() != null ? event.getHostClub().getName() : null)
                .membershipId(membership.getMembershipId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();

    }

    @Override
    public List<EventFeedbackResponse> getFeedbacksByEvent(Long eventId) {
        return feedbackRepo.findByEvent_EventId(eventId)
                .stream()
                .map(f -> EventFeedbackResponse.builder()
                        .feedbackId(f.getFeedbackId())
                        .eventId(f.getEvent().getEventId())
                        .eventName(f.getEvent().getName())
                        .clubName(f.getEvent().getHostClub() != null ? f.getEvent().getHostClub().getName() : null)
                        .membershipId(f.getMembership().getMembershipId())
                        .rating(f.getRating())
                        .comment(f.getComment())
                        .createdAt(f.getCreatedAt())
                        .build())
                .toList();
    }
    @Override
    public EventFeedbackResponse updateFeedback(Long feedbackId, EventFeedbackRequest req) {
        EventFeedback feedback = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feedback not found"));

        if (req.getRating() != null) feedback.setRating(req.getRating());
        if (req.getComment() != null) feedback.setComment(req.getComment());
        feedbackRepo.save(feedback);

        return EventFeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .eventId(feedback.getEvent().getEventId())
                .eventName(feedback.getEvent().getName())
                .clubName(feedback.getEvent().getHostClub() != null ? feedback.getEvent().getHostClub().getName() : null)
                .membershipId(feedback.getMembership().getMembershipId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    @Override
    public void deleteFeedback(Long feedbackId) {
        EventFeedback feedback = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feedback not found"));
        feedbackRepo.delete(feedback);
    }

    @Override
    public List<EventFeedbackResponse> getFeedbacksByMembership(Long membershipId) {
        return feedbackRepo.findByMembership_MembershipId(membershipId)
                .stream()
                .map(f -> EventFeedbackResponse.builder()
                        .feedbackId(f.getFeedbackId())
                        .eventId(f.getEvent().getEventId())
                        .eventName(f.getEvent().getName())
                        .clubName(f.getEvent().getHostClub() != null ? f.getEvent().getHostClub().getName() : null)
                        .membershipId(membershipId)
                        .rating(f.getRating())
                        .comment(f.getComment())
                        .createdAt(f.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public Map<String, Object> getFeedbackSummaryByEvent(Long eventId) {
        List<EventFeedback> feedbacks = feedbackRepo.findByEvent_EventId(eventId);
        double avgRating = feedbacks.stream()
                .mapToInt(EventFeedback::getRating)
                .average()
                .orElse(0.0);
        int total = feedbacks.size();

        return Map.of(
                "eventId", eventId,
                "averageRating", avgRating,
                "totalFeedbacks", total
        );
    }

}
