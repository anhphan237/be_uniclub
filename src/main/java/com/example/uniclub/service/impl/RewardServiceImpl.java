package com.example.uniclub.service.impl;

import com.example.uniclub.entity.User;
import com.example.uniclub.exception.ApiException;
import com.example.uniclub.repository.UserRepository;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {

    private final EmailService emailService;
    private final UserRepository userRepo;

    // ✅ Hàm tiện ích để format điểm
    private String formatPoints(long points) {
        // Dùng Locale.US → 1,000,000 ; Dùng Locale.GERMANY → 1.000.000
        return NumberFormat.getNumberInstance(Locale.US).format(points);
    }

    @Override
    public void sendCheckInRewardEmail(Long userId, String eventName, long pointsEarned, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        String formattedEarned = formatPoints(pointsEarned);
        String formattedTotal = formatPoints(totalPoints);

        emailService.sendEmail(
                user.getEmail(),
                "You’ve Earned UniPoints for Attending " + eventName + "!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Thank you for attending <b>%s</b>!</p>
                <p>You’ve just earned <b>%s UniPoints</b>.</p>
                <p>Your current total is <b>%s UniPoints</b>.</p>
                <p>Keep participating in events to earn more rewards.</p>
                """.formatted(user.getFullName(), eventName, formattedEarned, formattedTotal)
        );
    }

    @Override
    public void sendManualBonusEmail(Long userId, long bonusPoints, String reason, long totalPoints) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        String formattedBonus = formatPoints(bonusPoints);
        String formattedTotal = formatPoints(totalPoints);
        String r = (reason == null || reason.isBlank()) ? "your contribution" : reason;

        emailService.sendEmail(
                user.getEmail(),
                "You’ve Received Extra UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Good news! You’ve received <b>%s UniPoints</b> for <b>%s</b>.</p>
                <p>Your total balance is now <b>%s UniPoints</b>.</p>
                <p>Keep up the great work and continue to shine in UniClub!</p>
                """.formatted(user.getFullName(), formattedBonus, r, formattedTotal)
        );
    }

    @Override
    public void sendMilestoneEmail(Long userId, long milestone) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        String formattedMilestone = formatPoints(milestone);

        emailService.sendEmail(
                user.getEmail(),
                "Congratulations! You’ve Reached " + formattedMilestone + " UniPoints!",
                """
                <p>Dear <b>%s</b>,</p>
                <p>Congratulations on reaching <b>%s UniPoints</b>!</p>
                <p>This milestone reflects your consistent engagement with UniClub.</p>
                <p>Keep exploring more clubs and events to earn even greater rewards!</p>
                """.formatted(user.getFullName(), formattedMilestone)
        );
    }
}
