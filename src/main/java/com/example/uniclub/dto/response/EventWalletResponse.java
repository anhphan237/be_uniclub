package com.example.uniclub.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class EventWalletResponse {
    private Long eventId;
    private String eventName;
    private String hostClubName;
    private Integer budgetPoints;
    private Long balancePoints;
    private String ownerType;
    private LocalDateTime createdAt;
    private List<Transaction> transactions;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class Transaction {
        private Long id;
        private String type;
        private Integer amount;
        private String description;
        private LocalDateTime createdAt;
    }
}
