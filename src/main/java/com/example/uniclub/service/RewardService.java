package com.example.uniclub.service;

public interface RewardService {
    void sendCheckInRewardEmail(Long userId, String eventName, int pointsEarned, int totalPoints);
    void sendManualBonusEmail(Long userId, int bonusPoints, String reason, int totalPoints);
    void sendMilestoneEmail(Long userId, int milestone);
}
