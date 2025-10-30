package com.example.uniclub.dto.response;

import com.example.uniclub.enums.WalletOwnerTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletResponse {

    private Long walletId;
    private Long balancePoints;

    private WalletOwnerTypeEnum ownerType;

    // Thông tin cho ví CLB (nếu có)
    private Long clubId;
    private String clubName;

    // Thông tin cho ví User (nếu có)
    private Long userId;
    private String userFullName;


}
