package com.example.uniclub.dto.request;

import lombok.Data;

/**
 * Used when a club leader or vice leader sends an invitation
 * to a student to join the club.
 */
@Data
public class ClubInviteRequest {
    private Long clubId;      // ID of the club sending the invite
    private Long userId;      // Target user to invite
    private String message;   // Optional invitation message
}
