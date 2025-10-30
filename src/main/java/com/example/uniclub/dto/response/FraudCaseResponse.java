package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FraudCaseResponse {
    private Long registrationId;
    private String memberName;
    private String memberEmail;
    private LocalDateTime checkinAt;
    private LocalDateTime checkMidAt;
    private LocalDateTime checkoutAt;
    private String fraudReason;
}
