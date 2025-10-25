package com.example.uniclub.service;

import com.example.uniclub.entity.Redeem;

public interface RedeemService {
    Redeem redeemProduct(Long userId, Long productId, Integer quantity, Long eventId);
}
