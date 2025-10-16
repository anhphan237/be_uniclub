package com.example.uniclub.dto.request;

import lombok.Data;

@Data
public class MemberApplicationStatusUpdateRequest {
    private String status;   // e.g. "APPROVED", "REJECTED"
    private String note;
}
