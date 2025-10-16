package com.example.uniclub.dto.request;

import lombok.Data;

/**
 * Used when a club leader adds or edits an internal note on an application.
 */
@Data
public class MemberApplicationNotePatchRequest {
    private String note;
}
