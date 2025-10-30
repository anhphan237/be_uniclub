package com.example.uniclub.service;

import com.example.uniclub.dto.response.EventWalletResponse;
import com.example.uniclub.entity.Event;

public interface EventWalletService {
    void createEventWallet(Event event);
    void grantBudgetToEvent(Event event, long points);
    void returnSurplusToClubs(Event event);
    EventWalletResponse getEventWalletDetail(Long eventId);

}
