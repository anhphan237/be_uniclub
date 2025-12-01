package com.example.uniclub.service;

import com.example.uniclub.dto.response.*;
import com.example.uniclub.dto.response.*;

import java.util.List;

public interface ClubMonthlyActivityService {

    ClubMonthlyActivityResponse recalculateForClub(Long clubId, int year, int month);

    List<ClubMonthlyActivityResponse> recalculateAllClubs(int year, int month);

    ClubMonthlyActivityResponse getClubMonthlyActivity(Long clubId, int year, int month);

    List<ClubMonthlyActivityResponse> getClubRanking(int year, int month);

    boolean exists(Long clubId, int year, int month);

    void deleteMonthlyRecord(Long clubId, int year, int month);

    List<ClubTrendingResponse> getTrendingClubs(int year, int month);

    List<ClubMonthlyHistoryPoint> getClubHistory(Long clubId, int year);

    ClubMonthlyBreakdownResponse getBreakdown(Long clubId, int year, int month);

    ClubCompareResponse compareClubs(Long clubA, Long clubB, int year, int month);

    List<ClubEventContributionResponse> getEventContribution(Long clubId, int year, int month);

    ClubMonthlyActivityResponse lockMonthlyRecord(Long clubId, int year, int month);

    ClubRewardApprovalResponse approveRewardPoints(Long clubId, int year, int month);

}
