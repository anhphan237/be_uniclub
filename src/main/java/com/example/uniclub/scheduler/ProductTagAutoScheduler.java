package com.example.uniclub.scheduler;

import com.example.uniclub.service.ProductTagAutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductTagAutoScheduler {

    private final ProductTagAutoService productTagAutoService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runAutoTagUpdate() {
        log.info("🔁 [Scheduler] Starting auto-tag update for Products...");
        productTagAutoService.updateDynamicTags();
        log.info("✅ [Scheduler] Auto-tag update completed successfully.");
    }
}
