package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class EventExtendRequest {
    private Long dayId;              // ngày nào cần sửa
    private String newStartTime;     // "09:00"
    private String newEndTime;       // "18:00"
}

