package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.EventFeedbackRequest;
import com.example.uniclub.dto.response.EventFeedbackResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.security.CustomUserDetails;
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
    public EventFeedbackResponse createFeedback(Long eventId, EventFeedbackRequest req, CustomUserDetails userDetails) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        User user = userDetails.getUser();
        if (user == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "User not authenticated");

        Membership membership = membershipRepo.findByUserAndClub(user, event.getHostClub())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not a member of this club"));

        EventFeedback feedback = EventFeedback.builder()
                .event(event)
                .membership(membership)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        feedbackRepo.save(feedback);

        return EventFeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .eventId(event.getEventId())
                .eventName(event.getName())
                .clubName(event.getHostClub().getName())
                .memberName(user.getFullName())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }


    @Override
    public List<EventFeedbackResponse> getFeedbacksByEvent(Long eventId) {
        return feedbackRepo.findByEvent_EventId(eventId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public EventFeedbackResponse updateFeedback(Long feedbackId, EventFeedbackRequest req) {
        EventFeedback feedback = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Feedback not found"));

        if (req.getRating() != null) feedback.setRating(req.getRating());
        if (req.getComment() != null) feedback.setComment(req.getComment());

        feedbackRepo.save(feedback);
        return mapToResponse(feedback);
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
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Map<String, Object> getFeedbackSummaryByEvent(Long eventId) {
        List<EventFeedback> feedbacks = feedbackRepo.findByEvent_EventId(eventId);
        double avgRating = feedbacks.stream().mapToInt(EventFeedback::getRating).average().orElse(0.0);
        return Map.of("eventId", eventId, "averageRating", avgRating, "totalFeedbacks", feedbacks.size());
    }

    // ✅ Mapper dùng chung, giúp code ngắn và sạch
    private EventFeedbackResponse mapToResponse(EventFeedback f) {
        return EventFeedbackResponse.builder()
                .feedbackId(f.getFeedbackId())
                .eventId(f.getEvent().getEventId())
                .eventName(f.getEvent().getName())
                .clubName(f.getEvent().getHostClub() != null ? f.getEvent().getHostClub().getName() : null)
                .membershipId(f.getMembership().getMembershipId())
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }

    @Override
    public List<EventFeedbackResponse> getFeedbacksByClub(Long clubId) {
        List<EventFeedback> feedbacks = feedbackRepo.findAllByClubOrCoHost(clubId);
        return feedbacks.stream()
                .map(f -> EventFeedbackResponse.builder()
                        .feedbackId(f.getFeedbackId())
                        .eventId(f.getEvent().getEventId())
                        .eventName(f.getEvent().getName())
                        .clubName(f.getEvent().getHostClub().getName())
                        .membershipId(f.getMembership().getMembershipId())
                        .memberName(f.getMembership().getUser().getFullName()) // ✅ lấy tên người gửi
                        .rating(f.getRating())
                        .comment(f.getComment())
                        .createdAt(f.getCreatedAt())
                        .updatedAt(f.getUpdatedAt())
                        .build())
                .toList();
    }
    @Override
    public List<EventFeedbackResponse> getFeedbacksByUser(Long userId) {
        return feedbackRepo.findByMembership_User_UserId(userId)
                .stream()
                .map(EventFeedbackResponse::fromEntity)
                .toList();
    }


}
