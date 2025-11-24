package com.example.uniclub.service;

import com.example.uniclub.dto.response.CalculateLiveActivityResponse;
import com.example.uniclub.dto.response.CalculateScoreResponse;
import com.example.uniclub.dto.response.PerformanceDetailResponse;
import com.example.uniclub.entity.MemberMonthlyActivity;

import java.util.List;

public interface ActivityEngineService {


    MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month);
    List<CalculateLiveActivityResponse> calculateLiveActivities(Long clubId, int attendanceBase, int staffBase);


    CalculateScoreResponse calculatePreviewScore(Long membershipId, int attendanceBase, int staffBase);


    void recalculateForClub(Long clubId, int year, int month);

    void recalculateAllForMonth(int year, int month);


    double calculateMemberScore(Long memberId);


    PerformanceDetailResponse calculateMemberScoreDetail(Long memberId);


    MemberMonthlyActivity getMonthlyActivity(Long memberId, int year, int month);


    List<MemberMonthlyActivity> getClubMonthlyActivities(Long clubId, int year, int month);


    List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month);
}
