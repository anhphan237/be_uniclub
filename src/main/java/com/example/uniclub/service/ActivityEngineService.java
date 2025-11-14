package com.example.uniclub.service;

import com.example.uniclub.entity.MemberMonthlyActivity;

public interface ActivityEngineService {

    // Tính lại cho 1 membership trong 1 tháng
    MemberMonthlyActivity recalculateForMembership(Long membershipId, int year, int month);

    // Tính lại cho cả CLB trong 1 tháng
    void recalculateForClub(Long clubId, int year, int month);

    // Tính lại toàn bộ (tất cả CLB) trong 1 tháng
    void recalculateAllForMonth(int year, int month);
}
