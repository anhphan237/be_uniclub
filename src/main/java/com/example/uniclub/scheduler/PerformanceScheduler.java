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
public class PerformanceScheduler {

    private final ActivityEngineService activityEngine;

    /**
     * 🔥 Scheduler chạy vào 00:10 ngày 1 mỗi tháng
     * Cron: second minute hour day-of-month month day-of-week
     *
     * Giải thích:
     *  - 0 10 0 1 * *   => chạy lúc 00:10 ngày mùng 1
     */
    @Scheduled(cron = "0 10 0 1 * *")
    public void calculateLastMonthPerformance() {

        LocalDate now = LocalDate.now();

        int year = now.getYear();
        int month = now.getMonthValue() - 1;

        // Nếu đang là tháng 1 → tính tháng 12 năm trước
        if (month == 0) {
            month = 12;
            year -= 1;
        }

        log.warn("===== PERFORMANCE MONTHLY SCHEDULER START ({}/{}) =====", month, year);

        try {
            activityEngine.recalculateAllForMonth(year, month);
            log.warn("===== PERFORMANCE MONTHLY SCHEDULER DONE ({}/{}) =====", month, year);

        } catch (Exception ex) {
            log.error("Scheduler failed for {}/{}: {}", month, year, ex.getMessage());
            ex.printStackTrace();
        }
    }


    /**
     * ⚡ Scheduler test (chạy mỗi 1 phút)
     * 👉 chỉ bật khi cần debug
     */
    // @Scheduled(fixedRate = 60000)
    public void debugRunEveryMinute() {
        LocalDate now = LocalDate.now();
        int y = now.getYear();
        int m = now.getMonthValue();

        log.info("DEBUG SCHEDULER: recalc {}/{}", m, y);
        activityEngine.recalculateAllForMonth(y, m);
    }
}
