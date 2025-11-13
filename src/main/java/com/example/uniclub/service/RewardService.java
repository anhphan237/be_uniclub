package com.example.uniclub.service;

import com.example.uniclub.entity.Event;

public interface RewardService {
    void sendCheckInRewardEmail(Long userId, String eventName, long pointsEarned, long totalPoints);
    void sendManualBonusEmail(Long userId, long bonusPoints, String reason, long totalPoints);
    void sendMilestoneEmail(Long userId, long milestone);
    void autoSettleEvent(Event event);
    void sendClubTopUpEmail(Long userId, String clubName, long points, String reason);

    void sendClubWalletDeductionEmail(Long userId, String clubName, long points, String reason);

    void sendClubBatchDeductionSummaryEmail(Long userId, String clubName, long totalPoints, int memberCount, String reason);
}
