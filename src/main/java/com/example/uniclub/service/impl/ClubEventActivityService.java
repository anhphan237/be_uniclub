package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.ClubEventMonthlyActivityResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.MultiplierPolicy;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.PolicyActivityTypeEnum;
import com.example.uniclub.enums.PolicyTargetTypeEnum;
import com.example.uniclub.repository.EventDayRepository;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.MultiplierPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubEventActivityService {

    private final EventRepository eventRepo;
    private final EventDayRepository eventDayRepo;
    private final MultiplierPolicyRepository multiplierPolicyRepository;

    public ClubEventMonthlyActivityResponse getClubEventActivity(Long clubId, YearMonth ym) {

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 🔹 Lấy tất cả eventId có bất kỳ ngày nào trong tháng đó
        List<Long> eventIds = eventDayRepo.findEventIdsByClubAndMonth(clubId, start, end);

        if (eventIds.isEmpty()) {
            return ClubEventMonthlyActivityResponse.builder()
                    .clubId(clubId)
                    .year(ym.getYear())
                    .month(ym.getMonthValue())
                    .totalEvents(0)
                    .completedEvents(0)
                    .rejectedEvents(0)
                    .activityLevel("NONE")
                    .multiplier(0)
                    .finalScore(0)
                    .build();
        }

        List<Event> events = eventRepo.findAllById(eventIds);

        // ====== TÍNH SỐ LƯỢNG ======
        int total = events.size();

        int completed = (int) events.stream()
                .filter(e -> e.getStatus() == EventStatusEnum.COMPLETED)
                .count();

        int rejected = (int) events.stream()
                .filter(e -> e.getStatus() == EventStatusEnum.REJECTED)
                .count();

        // ====== LẤY MULTIPLIER ======
        List<MultiplierPolicy> policies = multiplierPolicyRepository
                .findByTargetTypeAndActivityTypeAndActiveTrue(
                        PolicyTargetTypeEnum.CLUB,
                        PolicyActivityTypeEnum.CLUB_EVENT_ACTIVITY
                );

        MultiplierPolicy policy = policies.stream()
                .filter(p -> p.getMinThreshold() <= completed && p.getMaxThreshold() >= completed)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No multiplier policy found"));

        double multiplier = policy.getMultiplier();
        String level = policy.getRuleName();
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

