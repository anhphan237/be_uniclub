package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEventResponse {
    private Long id;
    private String title;
    private String description;
    private String clubName;
    private String majorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EventStatusEnum status;
    private int totalParticipants;
}
