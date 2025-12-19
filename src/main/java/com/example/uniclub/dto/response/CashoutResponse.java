package com.example.uniclub.dto.response;

import com.example.uniclub.enums.CashoutStatusEnum;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashoutResponse {

    private Long id;

    private Long clubId;
    private String clubName;

    private Long requestedById;
    private String requestedByName;

    private Long pointsRequested;
    private Long cashAmount;

    private CashoutStatusEnum status;

    private String leaderNote;
    private String staffNote;

    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
}
