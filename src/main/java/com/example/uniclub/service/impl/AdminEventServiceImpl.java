package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.AdminEventResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.service.AdminEventService;
import com.example.uniclub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final EmailService emailService; // ✅ thêm dòng này

    @Override
    public Page<AdminEventResponse> getAllEvents(String keyword, Pageable pageable) {
        Page<Event> events = (keyword == null || keyword.isBlank())
                ? eventRepo.findAll(pageable)
                : eventRepo.findByNameContainingIgnoreCase(keyword, pageable); // ✅ dùng name
        return events.map(this::toResponse);
    }

    @Override
    public AdminEventResponse getEventDetail(Long id) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        return toResponse(event);
    }

    @Override
    public void approveEvent(Long id) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        event.setStatus(EventStatusEnum.APPROVED);
        eventRepo.save(event);

        // ✅ Gửi email thông báo phê duyệt
        if (event.getHostClub() != null && event.getHostClub().getLeader() != null) {
            var leader = event.getHostClub().getLeader();
            emailService.sendEmail(
                    leader.getEmail(),
                    "Event Approved: " + event.getName(),
                    "Dear " + leader.getFullName() + ",\n\n"
                            + "Your event \"" + event.getName() + "\" has been approved by UniClub Admin.\n"
                            + "Congratulations and good luck!\n\n"
                            + "Best regards,\nUniClub Admin Team"
            );
        }
    }

    @Override
    public void rejectEvent(Long id, String reason) {
        Event event = eventRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        event.setStatus(EventStatusEnum.REJECTED);
        eventRepo.save(event);

        if (event.getHostClub() != null && event.getHostClub().getLeader() != null) {
            var leader = event.getHostClub().getLeader();
            emailService.sendEmail(
                    leader.getEmail(),
                    "Event Rejected: " + event.getName(),
                    "Dear " + leader.getFullName() + ",\n\n"
                            + "Your event \"" + event.getName() + "\" has been rejected by the admin.\n"
                            + "Reason: " + reason + "\n\n"
                            + "Please review the event rules and resubmit if necessary.\n\n"
                            + "Best regards,\nUniClub Admin Team"
            );
        }
    }

    private AdminEventResponse toResponse(Event event) {
        int participants = regRepo.countByEvent_EventId(event.getEventId());

        return AdminEventResponse.builder()
                .id(event.getEventId())
                .title(event.getName())
                .description(event.getDescription())
                .clubName(event.getHostClub() != null ? event.getHostClub().getName() : null)
                .majorName(event.getHostClub() != null && event.getHostClub().getMajor() != null
                        ? event.getHostClub().getMajor().getName() : null)
                .startTime(event.getStartTime() != null ? event.getStartTime().atDate(event.getDate()) : null)
                .endTime(event.getEndTime() != null ? event.getEndTime().atDate(event.getDate()) : null)
                .status(event.getStatus())
                .totalParticipants(participants)
                .build();
    }
}
