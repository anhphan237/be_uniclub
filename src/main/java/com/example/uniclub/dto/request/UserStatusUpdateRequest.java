// File: src/main/java/com/example/uniclub/dto/request/UserStatusUpdateRequest.java
package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Dùng cho API: PUT /api/users/{id}/status
 * JSON ví dụ:
 * { "active": true }  // ACTIVE
 * { "active": false } // INACTIVE
 */
public record UserStatusUpdateRequest(
        @NotNull(message = "active is required")
        Boolean active
) {}
