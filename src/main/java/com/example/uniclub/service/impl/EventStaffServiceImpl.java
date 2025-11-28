package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.dto.response.StaffInfoResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventStaff;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.enums.EventStaffStateEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.EventStaffRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.EventStaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventStaffServiceImpl implements EventStaffService {

    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final EventStaffRepository eventStaffRepository;
    private final EmailService emailService;

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

        Optional<EventStaff> existingOpt =
                eventStaffRepository.findByEvent_EventIdAndMembership_MembershipId(eventId, membershipId);

        if (existingOpt.isPresent()) {
            EventStaff es = existingOpt.get();

            // Nếu trước đó bị REMOVED hoặc EXPIRED → kích hoạt lại
            if (es.getState() == EventStaffStateEnum.REMOVED || es.getState() == EventStaffStateEnum.EXPIRED) {

                es.setState(EventStaffStateEnum.ACTIVE);
                es.setDuty(duty);
                es.setAssignedAt(LocalDateTime.now());
                es.setUnassignedAt(null);

                EventStaff saved = eventStaffRepository.save(es);
                return EventStaffResponse.from(saved);
            }

            // Nếu đang ACTIVE → không cho assign lại
            if (es.getState() == EventStaffStateEnum.ACTIVE) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Member is already assigned as staff for this event");
            }
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

        // Gửi email – tránh rollback nếu lỗi
        try {
            emailService.sendEventStaffAssignmentEmail(
                    membership.getUser().getEmail(),
                    membership.getUser().getFullName(),
                    event,
                    duty
            );
        } catch (Exception e) {
            log.error("Failed to send staff assignment email", e);
        }

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

        // Lấy staff đang ACTIVE
        List<EventStaff> activeStaff =
                eventStaffRepository.findByEvent_EventIdAndState(eventId, EventStaffStateEnum.ACTIVE);

        // Nếu ACTIVE = 0 → đã chuyển EXPIRED từ lần trước → trả danh sách EXPIRED
        if (activeStaff.isEmpty()) {
            return eventStaffRepository.findByEvent_EventIdAndState(eventId, EventStaffStateEnum.EXPIRED);
        }

        // ACTIVE → EXPIRED
        LocalDateTime now = LocalDateTime.now();

        activeStaff.forEach(es -> {
            es.setState(EventStaffStateEnum.EXPIRED);
            es.setUnassignedAt(now);
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

        Event event = staff.getEvent();

        // ⭐ Không cho remove staff nếu event đã hoàn thành
        if (event.getStatus() == EventStatusEnum.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot remove staff from a COMPLETED event");
        }

        staff.setState(EventStaffStateEnum.REMOVED);
        staff.setUnassignedAt(LocalDateTime.now());

        eventStaffRepository.save(staff);
    }

    // ==========================================================
    // 4) COUNT FINISHED PARTICIPATION
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
    @Override
    public List<StaffInfoResponse> getMyActiveStaff(Long userId) {

        List<EventStaff> list = eventStaffRepository.findActiveStaffByUserId(userId);

        return list.stream()
                .map(es -> new StaffInfoResponse(
                        es.getEvent().getEventId(),
                        es.getEvent().getName(),
                        es.getEvent().getHostClub().getClubId(),
                        es.getEvent().getHostClub().getName(),
                        es.getDuty(),
                        es.getState()
                ))
                .toList();
    }

}
