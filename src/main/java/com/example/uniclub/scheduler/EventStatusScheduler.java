package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Event;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventRepository eventRepo;

    /**
     * ⏰ Scheduler chạy mỗi 5 phút để cập nhật trạng thái sự kiện.
     *  - APPROVED  → ONGOING (nếu đến giờ)
     *  - ONGOING   → COMPLETED (nếu quá giờ kết thúc)
     */
    @Scheduled(cron = "0 */5 * * * *") // Mỗi 5 phút
    @Transactional
    public void autoUpdateEventStatuses() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        log.info("⏰ Scheduler tick: {} at {}", today, now);

        // 1️⃣ APPROVED → ONGOING
        List<Event> approved = eventRepo.findAllByStatusAndDate(EventStatusEnum.APPROVED, today);
        for (Event e : approved) {
            if (now.isAfter(e.getStartTime())) { // ✅ Nới điều kiện: chỉ cần sau giờ bắt đầu
                e.setStatus(EventStatusEnum.ONGOING);
                eventRepo.save(e);
                log.info("🔵 Event {} - '{}' switched to ONGOING", e.getEventId(), e.getName());
            }
        }

        // 2️⃣ ONGOING → COMPLETED
        List<Event> ongoing = eventRepo.findAllByStatusAndDate(EventStatusEnum.ONGOING, today);
        for (Event e : ongoing) {
            if (now.isAfter(e.getEndTime())) {
                e.setStatus(EventStatusEnum.COMPLETED);
                eventRepo.save(e);
                log.info("🟣 Event {} - '{}' switched to COMPLETED", e.getEventId(), e.getName());
            }
        }
    }
}
