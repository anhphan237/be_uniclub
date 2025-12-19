package com.example.uniclub.dto.response;

import com.example.uniclub.enums.EventTypeEnum;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyEventAttendanceStatusResponse {

    private Long eventId;
    private EventTypeEnum eventType;

    private boolean registered;

    private boolean checkedInStart;
    private boolean checkedInMid;
    private boolean checkedInEnd;

    private boolean fullyCheckedIn;
}
