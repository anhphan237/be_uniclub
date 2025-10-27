package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCheckinRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventRegisterRequest;
import com.example.uniclub.security.CustomUserDetails;

import java.util.List;
import java.util.Map;

public interface EventPointsService {


    String register(CustomUserDetails principal, EventRegisterRequest req);

    String checkin(CustomUserDetails principal, EventCheckinRequest req);

    String cancelRegistration(CustomUserDetails principal, Long eventId);

    String endEvent(CustomUserDetails principal, EventEndRequest req);

    List<?> getEventRegistrations(Long eventId);

    Map<String, Object> getEventSummary(Long eventId);

    Map<String, Object> getEventWallet(Long eventId);
}
