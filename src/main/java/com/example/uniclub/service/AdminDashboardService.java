package com.example.uniclub.service;

import com.example.uniclub.dto.response.*;

import java.util.List;

public interface AdminDashboardService {

    AdminSummaryResponse getSummary();

    List<ClubRankingResponse> getClubRanking(int year, int month);

    List<EventRankingResponse> getEventRanking(Integer year, Integer month);

    SystemOverviewResponse getAdvancedOverview();

    List<RecommendationResponse> getRecommendations();

    List<RecommendationResponse> getAIRecommendations();
}
