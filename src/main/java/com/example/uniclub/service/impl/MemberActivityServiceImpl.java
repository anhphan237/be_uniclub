package com.example.uniclub.service.impl;

import com.example.uniclub.entity.MemberMonthlyActivity;
import com.example.uniclub.service.ActivityEngineService;
import com.example.uniclub.service.MemberActivityService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberActivityServiceImpl implements MemberActivityService {

    private final ActivityEngineService activityEngineService;

    /**
     * Recalculate activity for ALL clubs in a given month.
     * Ví dụ: month = YearMonth.of(2025, 11)
     */
    @Override
    @Transactional
    public void recalculateForAllClubsAndMonth(YearMonth month) {
        int year = month.getYear();
        int m = month.getMonthValue();
        log.info("Recalculate activity for ALL clubs in {}/{}", m, year);
        activityEngineService.recalculateAllForMonth(year, m);
    }

    /**
     * Recalculate activity cho 1 club trong 1 tháng.
     */
    @Override
    @Transactional
    public void recalculateForClubAndMonth(Long clubId, YearMonth month) {
        int year = month.getYear();
        int m = month.getMonthValue();
        log.info("Recalculate activity for club {} in {}/{}", clubId, m, year);
        activityEngineService.recalculateForClub(clubId, year, m);
    }

    /**
     * OPTIONAL helper: Recalculate cho 1 membership trong 1 tháng.
     * (Không bắt buộc nằm trong interface, nhưng dùng được ở chỗ khác nếu cần.)
     */
    @Transactional
    public MemberMonthlyActivity recalculateForMembership(Long membershipId, YearMonth month) {
        int year = month.getYear();
        int m = month.getMonthValue();
        log.info("Recalculate activity for membership {} in {}/{}", membershipId, m, year);
        return activityEngineService.recalculateForMembership(membershipId, year, m);
    }
}
