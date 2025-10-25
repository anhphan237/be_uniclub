package com.example.uniclub.job;

import com.example.uniclub.entity.EventStaff;
import com.example.uniclub.enums.EventStaffStateEnum;
import com.example.uniclub.repository.EventStaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventStaffExpireJob {

    private final EventStaffRepository eventStaffRepository;

    // Chạy mỗi giờ
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireStaffWhenEventEnds() {
        List<EventStaff> list = eventStaffRepository.findActiveWhereEventEnded();
        for (EventStaff es : list) {
            es.setState(EventStaffStateEnum.EXPIRED);
            es.setUnassignedAt(LocalDateTime.now());
        }
        eventStaffRepository.saveAll(list);
    }
}
