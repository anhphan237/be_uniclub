package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventValidityResponse {

    private Long productId;
    private Long eventId;

    private EventStatusEnum eventStatus;
    private boolean expired;

    private LocalDateTime expiredAt;

    private String message;

    public static EventValidityResponse ok(
            Long productId,
            Long eventId,
            EventStatusEnum status,
            boolean expired,
            LocalDateTime endAt
    ) {
        return EventValidityResponse.builder()
                .productId(productId)
                .eventId(eventId)
                .eventStatus(status)
                .expired(expired)
                .expiredAt(endAt)
                .message(expired ? "Event expired" : "Event still valid")
                .build();
    }

    public static EventValidityResponse notEventProduct(Long productId) {
        return EventValidityResponse.builder()
                .productId(productId)
                .expired(true)
                .message("This product is not an EVENT_ITEM")
                .build();
    }

    public static EventValidityResponse noEventLinked(Long productId) {
        return EventValidityResponse.builder()
                .productId(productId)
                .expired(true)
                .message("EVENT_ITEM but no event linked")
                .build();
    }
}
