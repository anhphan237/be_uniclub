package com.example.uniclub.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EventExtendRequest {
    private LocalDate newDate;
    private String newStartTime; // ví dụ: "08:30"
    private String newEndTime;   // ví dụ: "17:00"
    private String reason;
}
