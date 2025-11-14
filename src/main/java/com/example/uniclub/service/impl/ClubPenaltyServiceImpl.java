package com.example.uniclub.service.impl;

import com.example.uniclub.dto.request.CreateClubPenaltyRequest;
import com.example.uniclub.entity.*;
import com.example.uniclub.enums.ClubPenaltyTypeEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.*;
import com.example.uniclub.service.ClubPenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClubPenaltyServiceImpl implements ClubPenaltyService {

    private final MembershipRepository membershipRepo;
    private final EventRepository eventRepo;
    private final ClubPenaltyRepository clubPenaltyRepo;

    @Override
    @Transactional
    public ClubPenalty createPenalty(Long clubId,
                                     CreateClubPenaltyRequest request,
                                     User createdBy) {

        Membership membership = membershipRepo.findById(request.membershipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Membership not found."));

        if (!membership.getClub().getClubId().equals(clubId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Membership does not belong to this club.");
        }

        Event event = null;
        if (request.eventId() != null) {
            event = eventRepo.findById(request.eventId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found."));
        }

        Integer points = request.points();
        if (points == null) {
            points = defaultPointsFor(request.type());
        }

        if (points >= 0)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Penalty points must be negative.");

        ClubPenalty penalty = ClubPenalty.builder()
                .membership(membership)
                .event(event)
                .type(request.type())
                .points(points)
                .reason(request.reason())
                .createdBy(createdBy)
                .build();

        return clubPenaltyRepo.save(penalty);
    }

    private int defaultPointsFor(ClubPenaltyTypeEnum type) {
        return switch (type) {
            case UNEXCUSED_ABSENCE -> -5;
            case LATE -> -3;
            case UNFINISHED_TASK -> -10;
            case MISCONDUCT -> -15;
            case CHEATING -> -30;
            case REPEATED_VIOLATION -> -15;
            case DISOBEY_STAFF -> -5;
            case DAMAGE_PROPERTY -> -50;
        };
    }
}
