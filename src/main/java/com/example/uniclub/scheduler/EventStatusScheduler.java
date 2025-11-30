package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventDay;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
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
    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void autoUpdateEventStatuses() {

        LocalDateTime now = LocalDateTime.now();
        log.info("[Scheduler] Checking events at {}", now);

        // APPROVED → ONGOING
        List<Event> approvedEvents = eventRepo.findAllByStatus(EventStatusEnum.APPROVED);

        for (Event e : approvedEvents) {
            LocalDateTime start = getEventStart(e);
            if (start == null) continue;

            if (now.isAfter(start) || now.isEqual(start)) {
                e.setStatus(EventStatusEnum.ONGOING);
                log.info("Event {} - '{}' → ONGOING", e.getEventId(), e.getName());
            }
        }

        // ONGOING → COMPLETED
        List<Event> ongoingEvents = eventRepo.findAllByStatus(EventStatusEnum.ONGOING);

        for (Event e : ongoingEvents) {
            LocalDateTime end = getEventEnd(e);
            if (end == null) continue;

            if (now.isAfter(end)) {
                e.setStatus(EventStatusEnum.COMPLETED);
                e.setCompletedAt(LocalDateTime.now());
                log.info("Event {} - '{}' → COMPLETED", e.getEventId(), e.getName());
            }
        }

        eventRepo.saveAll(approvedEvents);
        eventRepo.saveAll(ongoingEvents);

        log.info("Scheduler done: {} approved, {} ongoing updated",
                approvedEvents.size(), ongoingEvents.size());
    }

    private LocalDateTime getEventStart(Event event) {
        if (event.getDays() == null || event.getDays().isEmpty()) return null;

        EventDay earliest = event.getDays().stream()
                .min(Comparator.comparing(EventDay::getDate)
                        .thenComparing(EventDay::getStartTime))
                .orElse(null);

        return LocalDateTime.of(earliest.getDate(), earliest.getStartTime());
    }

    private LocalDateTime getEventEnd(Event event) {
        if (event.getDays() == null || event.getDays().isEmpty()) return null;

        EventDay latest = event.getDays().stream()
                .max(Comparator.comparing(EventDay::getDate)
                        .thenComparing(EventDay::getEndTime))
                .orElse(null);

        return LocalDateTime.of(latest.getDate(), latest.getEndTime());
    }

}
