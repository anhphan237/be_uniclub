package com.example.uniclub.service;

import com.example.uniclub.dto.response.EventStaffResponse;
import java.util.List;

public interface EventStaffService {
    EventStaffResponse assignStaff(Long eventId, Long membershipId, String duty);
    void unassignStaff(Long eventStaffId);

long countStaffParticipation(Long membershipId);

}
