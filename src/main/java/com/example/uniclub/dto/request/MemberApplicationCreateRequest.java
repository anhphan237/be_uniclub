package com.example.uniclub.dto.request;

import lombok.Data;

/**
 * Used when a student submits a new membership application.
 */
@Data
public class MemberApplicationCreateRequest {
    private Long clubId;
    private String message;
}
