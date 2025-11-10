package com.example.uniclub.service;

import com.example.uniclub.dto.response.EventStatsResponse;
import com.example.uniclub.dto.response.FraudCaseResponse;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    Map<String, Object> getQrTokenForEvent(Long eventId, String phase);
    void checkInWithToken(String token, String email);
    String verifyAttendance(Long eventId, Long userId);
    void verifyAndSaveAttendance(User user, Event event, String level);
    void scanEventPhase(String tokenValue, String email);
    EventStatsResponse getEventStats(Long eventId);
    List<FraudCaseResponse> getFraudCases(Long eventId);
    void handleStartCheckin(User user, Event event);
    void handleMidCheckin(User user, Event event);
    void handleEndCheckout(User user, Event event);
    void handlePublicCheckin(User user, Event event);

}
