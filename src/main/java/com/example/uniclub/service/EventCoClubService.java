package com.example.uniclub.service;

import com.example.uniclub.enums.EventCoHostStatusEnum;

public interface EventCoClubService {
    void updateStatus(Long eventId, Long clubId, EventCoHostStatusEnum newStatus);

}
