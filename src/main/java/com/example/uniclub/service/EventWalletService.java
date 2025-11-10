package com.example.uniclub.service;

import com.example.uniclub.dto.response.EventWalletResponse;
import com.example.uniclub.entity.Event;

public interface EventWalletService {
    EventWalletResponse getEventWalletDetail(Long eventId);

}
