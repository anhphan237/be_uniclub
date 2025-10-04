package com.example.uniclub.dto.response;

import com.example.uniclub.entity.EventType;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class EventResponse {
    private Long id;
    private Long clubId;
    private String name;
    private String description;
    private EventType type;
    private LocalDate date;
    private LocalTime time;
    private Long locationId;
}
