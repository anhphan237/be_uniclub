package com.example.uniclub.scheduler;

import com.example.uniclub.service.ActivityEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyActivityScheduler {

    private final ActivityEngineService activityEngineService;

    // Chạy 3h sáng ngày mùng 1 hàng tháng
    @Scheduled(cron = "0 0 3 1 * *")
    public void calculateLastMonthActivity() {
        LocalDate now = LocalDate.now();
        LocalDate lastMonth = now.minusMonths(1);

        int year = lastMonth.getYear();
        int month = lastMonth.getMonthValue();

        log.info("Start monthly activity calculation for {}/{}", month, year);
        activityEngineService.recalculateAllForMonth(year, month);
        log.info("Done monthly activity calculation for {}/{}", month, year);
    }
}
