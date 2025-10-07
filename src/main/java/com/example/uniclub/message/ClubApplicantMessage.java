package com.example.uniclub.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClubApplicantMessage {
    private Long applicationId;
    private String clubName;
    private String reviewedBy;
    private String status;
}