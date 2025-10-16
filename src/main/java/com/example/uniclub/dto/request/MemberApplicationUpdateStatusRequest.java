package com.example.uniclub.dto.request;

import lombok.Data;

/**
 * Used when a club leader or vice leader updates an application’s status.
 */
@Data
public class MemberApplicationUpdateStatusRequest {
    private String status;   // APPROVED, REJECTED, INTERVIEWING, EXPIRED
    private String note;     // optional note or reason
}
