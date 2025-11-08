package com.example.uniclub.dto.response;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AdminWalletResponse {
    private Long id;
    private String ownerName;
    private String walletType;
    private Long balance;
}
