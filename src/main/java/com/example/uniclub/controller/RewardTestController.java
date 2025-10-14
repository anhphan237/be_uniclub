package com.example.uniclub.controller;

import com.example.uniclub.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reward-test")
@RequiredArgsConstructor
public class RewardTestController {

    private final RewardService rewardService;

    // 🧪 Test gửi email khi check-in
    @PostMapping("/checkin")
    public String testCheckin(@RequestParam Long userId) {
        rewardService.sendCheckInRewardEmail(userId, "Green Planet Festival", 10, 120);
        return "✅ Check-in reward email sent!";
    }

    // 🧪 Test gửi email thưởng điểm
    @PostMapping("/bonus")
    public String testBonus(@RequestParam Long userId) {
        rewardService.sendManualBonusEmail(userId, 20, "Volunteering in UniFair", 150);
        return "✅ Bonus reward email sent!";
    }

    // 🧪 Test gửi email khi đạt mốc điểm
    @PostMapping("/milestone")
    public String testMilestone(@RequestParam Long userId) {
        rewardService.sendMilestoneEmail(userId, 500);
        return "✅ Milestone reward email sent!";
    }
}
