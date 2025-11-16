package com.example.uniclub.service;

import com.example.uniclub.dto.response.PerformanceDetailResponse;
import com.example.uniclub.entity.MemberMonthlyActivity;

import java.util.List;

public interface ActivityEngineService {

    // Tính lại cho 1 membership trong 1 tháng
    MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month);

    // Tính lại cho cả CLB trong 1 tháng
    void recalculateForClub(Long clubId, int year, int month);

    // Tính lại toàn bộ (tất cả CLB) trong 1 tháng
    void recalculateAllForMonth(int year, int month);

    double calculateMemberScore(Long memberId);

    // Lấy chi tiết điểm (base, multiplier, final)
    PerformanceDetailResponse calculateMemberScoreDetail(Long memberId);

    MemberMonthlyActivity getMonthlyActivity(Long memberId, int year, int month);
    List<MemberMonthlyActivity> getClubMonthlyActivities(Long clubId, int year, int month);
    List<MemberMonthlyActivity> getClubRanking(Long clubId, int year, int month);



}
