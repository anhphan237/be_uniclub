package com.example.uniclub.service;

import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import java.util.List;
public interface WalletRewardService {
    Wallet getWalletByUserId(Long userId);
    Wallet rewardPointsByMembershipId(User operator, Long membershipId, int points, String reason);
    int rewardPointsByClubId(User operator, Long clubId, int points, String reason);
    Wallet topUpClubWallet(User operator, Long clubId, int points, String reason);
    List<WalletTransactionResponse> rewardMultipleClubs(WalletRewardBatchRequest req);
    List<WalletTransactionResponse> rewardMultipleMembers(WalletRewardBatchRequest req);

}
