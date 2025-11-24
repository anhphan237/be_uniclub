package com.example.uniclub.dto.response;

import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.ProductOrder;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemScanResponse {

    private Long userId;
    private String fullName;
    private String studentCode;
    private Long membershipId;
    private Long clubId;

    private Long walletBalance;

    private List<RedeemOrderMiniResponse> pendingOrders;

    public static RedeemScanResponse from(
            User user,
            Membership membership,
            Wallet wallet,
            List<ProductOrder> pendingOrders
    ) {
        return RedeemScanResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .studentCode(user.getStudentCode())
                .membershipId(membership.getMembershipId())
                .clubId(membership.getClub().getClubId())
                .walletBalance(wallet.getBalancePoints())
                .pendingOrders(
                        pendingOrders.stream()
                                .map(RedeemOrderMiniResponse::from)
                                .toList()
                )
                .build();
    }
}
