package com.example.uniclub.service;

import com.example.uniclub.dto.response.EventStaffResponse;
import com.example.uniclub.dto.response.StaffInfoResponse;
import com.example.uniclub.entity.EventStaff;

import java.util.List;

public interface EventStaffService {

    EventStaffResponse assignStaff(Long eventId, Long membershipId, String duty);

    void unassignStaff(Long eventStaffId);

    long countStaffParticipation(Long membershipId);

    /**
     * Chuyển staff ACTIVE → EXPIRED khi event COMPLETED.
     * Trả về danh sách EXPIRED.
     */
    List<EventStaff> expireStaffOfCompletedEvent(Long eventId);

    List<StaffInfoResponse> getMyActiveStaff(Long userId);

    List<EventStaffResponse> getCompletedEventStaff(Long eventId);

    List<EventStaffResponse> getAllStaffByEvent(Long eventId);
}
