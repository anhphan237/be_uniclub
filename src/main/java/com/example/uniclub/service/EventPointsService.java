package com.example.uniclub.service;

import com.example.uniclub.dto.request.EventCheckinRequest;
import com.example.uniclub.dto.request.EventEndRequest;
import com.example.uniclub.dto.request.EventRegisterRequest;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;
import com.example.uniclub.security.CustomUserDetails;

public interface EventPointsService {

    void refundCommitPoints(User user, long points, Event event);

    String register(CustomUserDetails principal, EventRegisterRequest req);

    String checkin(CustomUserDetails principal, EventCheckinRequest req);

    String cancelRegistration(CustomUserDetails principal, Long eventId);

    String endEvent(CustomUserDetails principal, EventEndRequest req);
}
