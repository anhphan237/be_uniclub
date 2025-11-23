package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.MemberRewardSuggestionResponse;
import com.example.uniclub.entity.MemberMonthlyActivity;
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

        int finalScore = m.getFinalScore();
        int suggested = 0;
        String reasoning;

        if (finalScore >= 160) {
            suggested = 50;
            reasoning = "Outstanding activity level (Member of the Month candidate).";
        } else if (finalScore >= 130) {
            suggested = 30;
            reasoning = "High performance and excellent participation.";
        } else if (finalScore >= 90) {
            suggested = 20;
            reasoning = "Positive and consistent member activity.";
        } else if (finalScore >= 50) {
            suggested = 10;
            reasoning = "Normal activity level, average engagement.";
        } else {
            suggested = 0;
            reasoning = "Low activity this month.";
        }

        return MemberRewardSuggestionResponse.builder()
                .membershipId(m.getMembership().getMembershipId())
                .fullName(m.getMembership().getUser().getFullName())
                .finalScore(finalScore)
                .suggestedPoints(suggested)
                .suggestionReason(reasoning)
                .build();
    }
}
