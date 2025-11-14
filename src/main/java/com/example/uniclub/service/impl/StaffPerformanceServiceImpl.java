package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.StaffPerformanceRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.StaffPerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffPerformanceServiceImpl implements StaffPerformanceService {

    private final MembershipRepository membershipRepo;
    private final EventRepository eventRepo;
    private final EventStaffRepository eventStaffRepo;
    private final StaffPerformanceRepository staffPerformanceRepo;

    @Override
    @Transactional
    public StaffPerformance createStaffPerformance(Long clubId,
                                                   StaffPerformanceRequest request,
                                                   User createdBy) {

        Membership membership = membershipRepo.findById(request.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found."));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership does not belong to this club.");
        }

        Event event = eventRepo.findById(request.eventId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));

        // Check this member is staff of this event
        EventStaff es = eventStaffRepo.findActiveByEventAndMembership(
                        event.getEventId(),
                        membership.getMembershipId())
                .orElseThrow(() ->
                        new ApiException(HttpStatus.BAD_REQUEST, "This member is not active staff of the event."));

        StaffPerformance perf = StaffPerformance.builder()
                .eventStaff(es)
                .membership(membership)
                .event(event)
                .performance(request.performance())
                .note(request.note())
                .build();

        return staffPerformanceRepo.save(perf);
    }
}
