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

    @Override
    public void sendCheckInRewardEmail(Long userId, String eventName, long pointsEarned, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        emailService.sendEmail(
                user.getEmail(),
                "🎁 You’ve Earned UniPoints for Attending " + eventName + "!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Thank you for attending <b>%s</b>!</p>
                <p>You’ve just earned <b>%d UniPoints</b>.</p>
                <p>Your current total is <b>%d UniPoints</b>.</p>
                <p>Keep participating in events to earn more rewards 🎉.</p>
                """.formatted(user.getFullName(), eventName, pointsEarned, totalPoints)
        );
    }

    @Override
    public void sendManualBonusEmail(Long userId, long bonusPoints, String reason, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        String r = (reason == null || reason.isBlank()) ? "your contribution" : reason;
        emailService.sendEmail(
                user.getEmail(),
                "💎 You’ve Received Extra UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Good news! You’ve received <b>%d UniPoints</b> for <b>%s</b>.</p>
                <p>Your total balance is now <b>%d UniPoints</b>.</p>
                <p>Keep up the great work and continue to shine in UniClub!</p>
                """.formatted(user.getFullName(), bonusPoints, r, totalPoints)
        );
    }

    @Override
    public void sendMilestoneEmail(Long userId, long milestone) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        emailService.sendEmail(
                user.getEmail(),
                "🏆 Congratulations! You’ve Reached " + milestone + " UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Congratulations on reaching <b>%d UniPoints</b> 🎉</p>
                <p>This milestone reflects your consistent engagement with UniClub.</p>
                <p>Keep exploring more clubs and events to earn even greater rewards!</p>
                """.formatted(user.getFullName(), milestone)
        );
    }
}
