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
     * 🕒 Tự động cập nhật trạng thái sự kiện mỗi 5 phút:
     * - APPROVED → ONGOING nếu đã đến giờ bắt đầu
     * - ONGOING → COMPLETED nếu quá giờ kết thúc
     * - Bỏ qua event COMPLETED, CANCELED
     */
    @Scheduled(cron = "0 */5 * * * *") // mỗi 5 phút
    @Transactional
    public void autoUpdateEventStatuses() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        log.info("⏰ [Scheduler] Checking events at {} {}", today, now);

        // 1️⃣ APPROVED → ONGOING
        List<Event> approvedEvents = eventRepo.findAllByStatus(EventStatusEnum.APPROVED);
        for (Event e : approvedEvents) {
            boolean shouldStart =
                    e.getDate().isBefore(today) ||
                            (e.getDate().isEqual(today) && now.isAfter(e.getStartTime()));

            if (shouldStart) {
                e.setStatus(EventStatusEnum.ONGOING);
                log.info("🔵 Event {} - '{}' switched to ONGOING", e.getEventId(), e.getName());
            }
        }

        // 2️⃣ ONGOING → COMPLETED
        List<Event> ongoingEvents = eventRepo.findAllByStatus(EventStatusEnum.ONGOING);
        for (Event e : ongoingEvents) {
            boolean shouldEnd =
                    e.getDate().isBefore(today) ||
                            (e.getDate().isEqual(today) && now.isAfter(e.getEndTime()));

            if (shouldEnd) {
                e.setStatus(EventStatusEnum.COMPLETED);
                log.info("🟣 Event {} - '{}' switched to COMPLETED", e.getEventId(), e.getName());
            }
        }

        eventRepo.saveAll(approvedEvents);
        eventRepo.saveAll(ongoingEvents);

        log.info("✅ Scheduler done: {} approved, {} ongoing processed",
                approvedEvents.size(), ongoingEvents.size());
    }
}
