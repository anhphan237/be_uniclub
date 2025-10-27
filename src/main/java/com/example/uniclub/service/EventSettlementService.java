package com.example.uniclub.service;

import com.example.uniclub.entity.Event;

public interface EventSettlementService {
    /** ⚖️ Quyết toán điểm sự kiện khi kết thúc */
    void settleEvent(Event event);
}
