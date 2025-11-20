package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventStatusEnum;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventProductResponse {

    private Long productId;
    private String name;
    private Long pointCost;

    private Long eventId;
    private String eventName;
    private EventStatusEnum eventStatus;

    private boolean expired;
}
