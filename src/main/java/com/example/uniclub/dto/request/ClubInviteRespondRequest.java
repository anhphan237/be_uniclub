package com.example.uniclub.dto.request;

import lombok.Data;

/**
 * Used when a student accepts or declines a club invitation.
 */
@Data
public class ClubInviteRespondRequest {
    private String response; // ACCEPT or DECLINE
}
