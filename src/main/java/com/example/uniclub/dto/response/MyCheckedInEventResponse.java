package com.example.uniclub.dto.response;

import com.example.uniclub.enums.AttendanceLevelEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MyCheckedInEventResponse {

    private Long eventId;
    private String eventName;

    private boolean startChecked;
    private boolean midChecked;
    private boolean endChecked;

    private AttendanceLevelEnum attendanceLevel;
}
