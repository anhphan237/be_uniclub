// NEW
package com.example.uniclub.scheduler;

import com.example.uniclub.service.MemberActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
@RequiredArgsConstructor
public class MemberActivityScheduler {

    private final MemberActivityService memberActivityService;

    // Chạy 03:00 sáng ngày 1 mỗi tháng → tính cho THÁNG TRƯỚC
    @Scheduled(cron = "0 0 3 1 * ?")
    public void calculatePreviousMonth() {
        YearMonth previous = YearMonth.now().minusMonths(1);
        memberActivityService.recalculateForAllClubsAndMonth(previous);
    }
}
