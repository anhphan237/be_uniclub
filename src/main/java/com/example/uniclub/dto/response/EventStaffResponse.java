package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventStaffStateEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStaffResponse {

    private Long id;
    private Long eventId;
    private String eventName;
    private Long membershipId;
    private String memberName;
    private String duty;
    private EventStaffStateEnum state;
    private LocalDateTime assignedAt;
    private LocalDateTime unassignedAt;
}
