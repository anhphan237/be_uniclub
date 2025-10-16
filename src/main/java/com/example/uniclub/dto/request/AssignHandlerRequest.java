package com.example.uniclub.dto.request;

import lombok.Data;

/**
 * Used when a club leader assigns another user (usually vice leader)
 * to handle a specific membership application.
 */
@Data
public class AssignHandlerRequest {
    private Long handlerUserId;  // ID of the new handler (vice leader)
}
