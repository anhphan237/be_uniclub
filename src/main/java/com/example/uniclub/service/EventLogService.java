package com.example.uniclub.service;

import com.example.uniclub.entity.EventLog;
import com.example.uniclub.enums.UserActionEnum;

import java.util.List;

public interface EventLogService {
    void logAction(Long userId, String userName, Long eventId, String eventName,
                   UserActionEnum action, String description);

    List<EventLog> getLogsByEvent(Long eventId);
    List<EventLog> getLogsByUser(Long userId);
}
