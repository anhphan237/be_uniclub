package com.example.uniclub.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedeemRequest {
    private Long productId;
    private Integer quantity;
    private Long eventId; // optional (nếu đổi quà trong event)
}