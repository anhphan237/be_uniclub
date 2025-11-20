package com.example.uniclub.dto.response;

import com.example.uniclub.entity.EventStaff;
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

    public static EventStaffResponse from(EventStaff es) {
        if (es == null) return null;

        return EventStaffResponse.builder()
                .id(es.getId())
                .eventId(es.getEvent().getEventId())
                .eventName(es.getEvent().getName())
                .membershipId(es.getMembership().getMembershipId())
                .memberName(
                        es.getMembership() != null && es.getMembership().getUser() != null
                                ? es.getMembership().getUser().getFullName()
                                : null
                )
                .duty(es.getDuty())
                .state(es.getState())
                .assignedAt(es.getAssignedAt())
                .unassignedAt(es.getUnassignedAt())
                .build();
    }
}
