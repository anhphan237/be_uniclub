package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.EventStaffStateEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.EventStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventStaffServiceImpl implements EventStaffService {

    private final EventRepository eventRepository;
    private final MembershipRepository membershipRepository;
    private final EventStaffRepository eventStaffRepository;

    @Override
    @Transactional
    public EventStaffResponse assignStaff(Long eventId, Long membershipId, String duty) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));

        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found"));

        // ❌ Nếu event bị hủy hoặc bị reject
        if (event.getStatus() == EventStatusEnum.CANCELLED || event.getStatus() == EventStatusEnum.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot assign staff to a cancelled or rejected event");
        }

        // ❌ Nếu event đã kết thúc
        boolean eventEnded = event.getDate().isBefore(LocalDate.now())
                || (event.getDate().isEqual(LocalDate.now()) && event.getEndTime().isBefore(LocalTime.now()));
        if (eventEnded) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot assign staff to an event that has already ended");
        }

        // ⚠️ Nếu đã gán staff rồi
        boolean alreadyAssigned = eventStaffRepository.existsByEvent_EventIdAndMembership_MembershipId(eventId, membershipId);
        if (alreadyAssigned) {
            throw new ApiException(HttpStatus.CONFLICT, "This member is already assigned as staff for this event");
        }

        // ✅ Tạo staff mới
        EventStaff saved = eventStaffRepository.save(EventStaff.builder()
                .event(event)
                .membership(membership)
                .duty(duty)
                .state(EventStaffStateEnum.ACTIVE)
                .assignedAt(java.time.LocalDateTime.now())
                .build());

        // ✅ Map sang DTO
        return EventStaffResponse.builder()
                .id(saved.getId())
                .eventId(event.getEventId())
                .eventName(event.getName())
                .membershipId(membership.getMembershipId())
                .memberName(membership.getUser() != null ? membership.getUser().getFullName() : null)
                .duty(saved.getDuty())
                .state(saved.getState())
                .assignedAt(saved.getAssignedAt())
                .unassignedAt(saved.getUnassignedAt())
                .build();
    }

    @Override
    @Transactional
    public void unassignStaff(Long eventStaffId) {
        EventStaff staff = eventStaffRepository.findById(eventStaffId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EventStaff not found"));

        staff.setState(EventStaffStateEnum.REMOVED);
        staff.setUnassignedAt(java.time.LocalDateTime.now());
        eventStaffRepository.save(staff);
    }
//    @Override
//    public List<EventStaffResponse> getEventStaffList(Long eventId) {
//        Event event = eventRepository.findById(eventId)
//                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
//
//        List<EventStaff> staffs = eventStaffRepository.findByEvent_EventId(eventId);
//
//        return staffs.stream().map(staff -> EventStaffResponse.builder()
//                .id(staff.getId())
//                .eventId(event.getEventId())
//                .eventName(event.getName())
//                .membershipId(staff.getMembership().getMembershipId())
//                .memberName(staff.getMembership().getUser() != null
//                        ? staff.getMembership().getUser().getFullName() : null)
//                .duty(staff.getDuty())
//                .state(staff.getState())
//                .assignedAt(staff.getAssignedAt())
//                .unassignedAt(staff.getUnassignedAt())
//                .build()
//        ).toList();
//    }

}
