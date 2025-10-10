package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.enums.EventTypeEnum;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private Long clubId;
    private String name;
    private String description;
    private EventTypeEnum type;
    private LocalDate date;
    private String time;
    private EventStatusEnum status;
    private Long locationId;
    private String checkInCode;

}
