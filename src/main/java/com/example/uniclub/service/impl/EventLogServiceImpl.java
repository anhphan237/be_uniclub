package com.example.uniclub.service.impl;

import com.example.uniclub.entity.EventLog;
import com.example.uniclub.enums.UserActionEnum;
import com.example.uniclub.repository.EventLogRepository;
import com.example.uniclub.service.EventLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventLogServiceImpl implements EventLogService {

    private final EventLogRepository repo;

    @Override
    public void logAction(Long userId, String userName, Long eventId, String eventName,
                          UserActionEnum action, String description) {
        repo.save(EventLog.builder()
                .userId(userId)
                .userName(userName)
                .eventId(eventId)
                .eventName(eventName)
                .action(action)
                .description(description)
                .build());
    }

    @Override
    public List<EventLog> getLogsByEvent(Long eventId) {
        return repo.findByEventIdOrderByCreatedAtDesc(eventId);
    }

    @Override
    public List<EventLog> getLogsByUser(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
