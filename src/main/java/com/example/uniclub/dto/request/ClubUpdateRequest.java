package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class ClubUpdateRequest {

    private String name;
    private String description;
    private Long majorId;
    private String vision;

    // admin/staff mới được chỉnh
    private Double clubMultiplier;
    private String activityStatus;

    // đổi leader
    private Long newLeaderId;
}
