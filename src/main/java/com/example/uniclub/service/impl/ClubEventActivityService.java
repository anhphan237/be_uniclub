package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ClubEventMonthlyActivityResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubEventActivityService {

    private final EventRepository eventRepo;
    private final MultiplierPolicyRepository multiplierPolicyRepository;

    public ClubEventMonthlyActivityResponse getClubEventActivity(Long clubId, YearMonth ym) {

        List<Event> events = eventRepo.findByHostClub_ClubIdAndDateBetween(
                clubId,
                ym.atDay(1),
                ym.atEndOfMonth()
        );

        int total = events.size();
        int completed = (int) events.stream()
                .filter(e -> e.getStatus() == EventStatusEnum.COMPLETED)
                .count();
        int rejected = (int) events.stream()
                .filter(e -> e.getStatus() == EventStatusEnum.REJECTED)
                .count();

        List<MultiplierPolicy> multiplierPolicies = multiplierPolicyRepository
                .findByTargetTypeAndActivityTypeAndActiveTrue(PolicyTargetTypeEnum.CLUB, PolicyActivityTypeEnum.CLUB_EVENT_ACTIVITY);

        MultiplierPolicy multiplierPolicy = multiplierPolicies.stream()
                .filter(p -> p.getMinThreshold() <= completed && p.getMaxThreshold() >= completed)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No multiplier policy found"));;

        double multiplier = multiplierPolicy.getMultiplier();
        String level = multiplierPolicy.getRuleName();
        double finalScore = completed * multiplier;

        return ClubEventMonthlyActivityResponse.builder()
                .clubId(clubId)
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .totalEvents(total)
                .completedEvents(completed)
                .rejectedEvents(rejected)
                .activityLevel(level)
                .multiplier(multiplier)
                .finalScore(finalScore)
                .build();
    }
}

