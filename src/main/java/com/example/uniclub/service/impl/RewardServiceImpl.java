package com.example.uniclub.service.impl;

import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final EmailService emailService;
    private final UserRepository userRepo;

    // 🟢 Khi student check-in thành công
    @Override
    public void sendCheckInRewardEmail(Long userId, String eventName, int pointsEarned, int totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        emailService.sendEmail(
                user.getEmail(),
                "🎁 You’ve Earned UniPoints for Attending " + eventName + "!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Thank you for attending the event <b>%s</b>!</p>
                <p>You’ve just earned <b>%d UniPoints</b> for your participation.</p>
                <p>Your current total is <b>%d UniPoints</b>.</p>
                <p>Keep engaging with UniClub to unlock exciting rewards and exclusive offers 🎉.</p>
                """.formatted(user.getFullName(), eventName, pointsEarned, totalPoints)
        );
    }

    // 💎 Khi admin/staff cộng điểm thủ công
    @Override
    public void sendManualBonusEmail(Long userId, int bonusPoints, String reason, int totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        emailService.sendEmail(
                user.getEmail(),
                "💎 You’ve Received Extra UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Good news! You’ve received <b>%d UniPoints</b> bonus for <b>%s</b>.</p>
                <p>Your total balance is now <b>%d UniPoints</b>.</p>
                <p>Keep up your great contributions and continue to grow with UniClub!</p>
                """.formatted(user.getFullName(), bonusPoints, reason, totalPoints)
        );
    }

    // 🏆 Khi đạt mốc điểm quan trọng
    @Override
    public void sendMilestoneEmail(Long userId, int milestone) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        emailService.sendEmail(
                user.getEmail(),
                "🏆 Congratulations! You’ve Reached " + milestone + " UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Congratulations on reaching <b>%d UniPoints</b>! 🎉</p>
                <p>This achievement shows your consistent engagement and contribution to the UniClub community.</p>
                <p>Keep exploring more clubs and events to earn even more rewards!</p>
                """.formatted(user.getFullName(), milestone)
        );
    }
}
