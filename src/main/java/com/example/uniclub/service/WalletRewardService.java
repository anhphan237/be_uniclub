package com.example.uniclub.service;

import com.example.uniclub.dto.request.WalletRewardBatchRequest;
import com.example.uniclub.dto.response.WalletTransactionResponse;
import com.example.uniclub.entity.User;
import com.example.uniclub.entity.Wallet;
import java.util.List;

public interface WalletRewardService {
    Wallet getWalletByUserId(Long userId);
    Wallet rewardPointsByUser(User operator, Long userId, long points, String reason);

    int rewardPointsByClubId(User operator, Long clubId, long points, String reason);
    Wallet topUpClubWallet(User operator, Long clubId, long points, String reason);
    List<WalletTransactionResponse> rewardMultipleClubs(WalletRewardBatchRequest req);
    List<WalletTransactionResponse> rewardMultipleMembers(WalletRewardBatchRequest req);

}
