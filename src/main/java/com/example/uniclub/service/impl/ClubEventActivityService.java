package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ClubEventMonthlyActivityResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.ClubEventActivityEnum;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubEventActivityService {

    private final EventRepository eventRepo;
    private final ClubRepository clubRepo;

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

        ClubEventActivityEnum level = ClubEventActivityEnum.classify(completed);
        double multiplier = level.multiplier;
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

