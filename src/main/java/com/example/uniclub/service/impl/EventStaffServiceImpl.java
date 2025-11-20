package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventStaff;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.EventStaffStateEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.EventStaffRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.EventStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventStaffServiceImpl implements EventStaffService {

    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final EventStaffRepository eventStaffRepository;

    // ==========================================================
    // 1) ASSIGN STAFF
    // ==========================================================
    @Override
    @Transactional
    public EventStaffResponse assignStaff(Long eventId, Long membershipId, String duty) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Staff can only be assigned when event is APPROVED");
        }

        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        boolean exists = eventStaffRepository
                .existsByEvent_EventIdAndMembership_MembershipId(eventId, membershipId);

        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This member is already assigned for this event");
        }

        EventStaff saved = eventStaffRepository.save(
                EventStaff.builder()
                        .event(event)
                        .membership(membership)
                        .duty(duty)
                        .state(EventStaffStateEnum.ACTIVE)
                        .assignedAt(LocalDateTime.now())
                        .build()
        );

        return EventStaffResponse.from(saved);
    }

    // ==========================================================
    // 2) EXPIRE STAFF WHEN EVENT COMPLETED
    // ==========================================================
    @Override
    @Transactional
    public List<EventStaff> expireStaffOfCompletedEvent(Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() != EventStatusEnum.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Event must be COMPLETED");
        }

        // Lấy staff ACTIVE
        List<EventStaff> activeStaff =
                eventStaffRepository.findByEvent_EventIdAndState(eventId, EventStaffStateEnum.ACTIVE);

        // Nếu ACTIVE = 0 → đã expire rồi → trả về EXPIRED
        if (activeStaff.isEmpty()) {
            return eventStaffRepository.findByEvent_EventIdAndState(eventId, EventStaffStateEnum.EXPIRED);
        }

        // ACTIVE → EXPIRED
        activeStaff.forEach(es -> {
            es.setState(EventStaffStateEnum.EXPIRED);
            es.setUnassignedAt(LocalDateTime.now());
        });

        return eventStaffRepository.saveAll(activeStaff);
    }

    // ==========================================================
    // 3) REMOVE STAFF
    // ==========================================================
    @Override
    @Transactional
    public void unassignStaff(Long eventStaffId) {

        EventStaff staff = eventStaffRepository.findById(eventStaffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EventStaff not found"));

        staff.setState(EventStaffStateEnum.REMOVED);
        staff.setUnassignedAt(LocalDateTime.now());

        eventStaffRepository.save(staff);
    }

    // ==========================================================
    // 4) COUNT PARTICIPATION
    // ==========================================================
    @Override
    public long countStaffParticipation(Long membershipId) {
        return eventStaffRepository.countByMembership_MembershipIdAndState(
                membershipId,
                EventStaffStateEnum.EXPIRED   // staff đã hoàn thành nhiệm vụ
        );
    }

    // ==========================================================
    // 5) GET COMPLETED EVENT STAFF
    // ==========================================================
    @Override
    @Transactional
    public List<EventStaffResponse> getCompletedEventStaff(Long eventId) {

        List<EventStaff> expired = expireStaffOfCompletedEvent(eventId);

        return expired.stream()
                .map(EventStaffResponse::from)
                .toList();
    }
}
