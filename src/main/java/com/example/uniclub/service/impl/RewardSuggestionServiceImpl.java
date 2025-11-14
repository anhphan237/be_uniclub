package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.MemberRewardSuggestionResponse;
import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.enums.MemberActivityLevelEnum;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.ClubRepository;
import com.example.uniclub.repository.MemberMonthlyActivityRepository;
import com.example.uniclub.service.RewardSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardSuggestionServiceImpl implements RewardSuggestionService {

    private final MemberMonthlyActivityRepository monthlyRepo;
    private final ClubRepository clubRepo;

    @Override
    public List<MemberRewardSuggestionResponse> getRewardSuggestions(Long clubId, int year, int month) {

        if (!clubRepo.existsById(clubId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Club not found");
        }

        List<MemberMonthlyActivity> list = monthlyRepo
                .findByMembership_Club_ClubIdAndYearAndMonth(clubId, year, month);

        return list.stream()
                .map(this::mapToSuggestion)
                .collect(Collectors.toList());
    }

    private MemberRewardSuggestionResponse mapToSuggestion(MemberMonthlyActivity m) {

        double finalScore = m.getFinalScore();
        MemberActivityLevelEnum level = m.getActivityLevel();

        int suggested = 0;
        String reasoning = "No reward recommended.";

        switch (level) {

            case MEMBER_OF_MONTH -> {
                suggested = 50;
                reasoning = "Top performer of the month.";
            }

            case FULL -> {
                if (finalScore >= 0.90) {
                    suggested = 30;
                } else if (finalScore >= 0.80) {
                    suggested = 20;
                } else {
                    suggested = 10;
                }
                reasoning = "High consistency with full activity performance.";
            }

            case POSITIVE -> {
                if (finalScore >= 0.75) {
                    suggested = 15;
                } else if (finalScore >= 0.60) {
                    suggested = 10;
                } else {
                    suggested = 5;
                }
                reasoning = "Good engagement and positive participation.";
            }

            case NORMAL -> {
                if (finalScore >= 0.50) {
                    suggested = 5;
                } else if (finalScore >= 0.35) {
                    suggested = 3;
                } else {
                    suggested = 1;
                }
                reasoning = "Average activity, room for improvement.";
            }

            case LOW -> {
                suggested = 0;
                reasoning = "Low activity this month.";
            }

            default -> {
                suggested = 0;
                reasoning = "No reward recommended.";
            }
        }

        return MemberRewardSuggestionResponse.builder()
                .membershipId(m.getMembership().getMembershipId())
                .fullName(m.getMembership().getUser().getFullName())
                .activityLevel(level)
                .baseScore(m.getBaseScore())
                .finalScore(finalScore)
                .suggestedPoints(suggested)
                .suggestionReason(reasoning)
                .build();
    }
}
