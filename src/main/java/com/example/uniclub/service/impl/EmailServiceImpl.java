package com.example.uniclub.service.impl;

import com.example.uniclub.entity.*;
import com.example.uniclub.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // ============================================================
    //  🔥 TEMPLATE – LIGHT + DARK MODE (AUTO DETECT)
    // ============================================================
    private String wrapTemplate(String innerHtml) {
        return """
        <div style="font-family: 'Inter', Arial, sans-serif; padding:32px; margin:auto; background:#F5F9FF;">
            <style>
                @media (prefers-color-scheme: dark) {
                    body, .email-body {
                        background:#0D1117 !important;
                        color:#E6EDF3 !important;
                    }
                    .email-card {
                        background:#161B22 !important;
                        border:1px solid #30363D !important;
                        color:#E6EDF3 !important;
                    }
                    .divider {
                        border-top:1px solid #30363D !important;
                    }
                    .footer-text {
                        color:#9BA3B4 !important;
                    }
                    .brand-text {
                        color:#58A6FF !important;
                    }
                    .otp-box {
                        background:#21262D !important;
                        color:#79C0FF !important;
                        border:1px solid #30363D !important;
                    }
                }
            </style>

            <div class="email-card" style="
                background:white;
                max-width:650px;
                margin:auto;
                border-radius:14px;
                padding:32px;
                box-shadow:0 4px 20px rgba(0,0,0,0.06);
            ">

                <div style="text-align:center; margin-bottom:24px;">
                    <img src='cid:uniclub-logo'
                         alt='UniClub Logo'
                         style="width:120px; opacity:0.95;">
                </div>

                <div style="font-size:15px; line-height:1.7; color:#222;">
                    %s
                </div>

                <hr class="divider"
                    style="border:none; border-top:1px solid #E0E7F1; margin:32px 0;">

                <div class="footer-text"
                    style="text-align:center; color:#777; font-size:13px; line-height:1.5;">
                    Best regards,<br>
                    <b class="brand-text" style="color:#1E88E5;">UniClub Vietnam</b><br>
                    Digitalizing Student Communities ✨
                </div>
            </div>
        </div>
        """.formatted(innerHtml);
    }

    // ============================================================
    //  📩 HÀM GỬI EMAIL CHUẨN
    // ============================================================
    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom("noreply@uniclub.id.vn", "UniClub Vietnam");
            helper.setTo(to);
            helper.setSubject(subject);

            // ✨ Apply template
            helper.setText(wrapTemplate(content), true);

            helper.addInline("uniclub-logo", new ClassPathResource("static/images/logo.png"));

            mailSender.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    //  🎯 TOÀN BỘ EMAIL PHÍA DƯỚI GIỮ NGUYÊN
    // ============================================================

    @Override
    public void sendFeedbackThankYouEmail(String to, String eventName, int rating) {
        String content = """
            <h2 style="color:#1E88E5; margin-top:0;">Thank you for your feedback! ⭐</h2>
            <p>Thank you for participating in the event <b>%s</b>.</p>
            <p>Your rating: <b>%d stars</b>.</p>
            <p>Your input helps us improve future UniClub events!</p>
        """.formatted(eventName, rating);

        sendEmail(to, "Thank You for Your Feedback!", content);
    }

    @Override
    public void sendWelcomeEmail(String to, String fullName) {
        String content = """
            <h2 style="color:#1E88E5;">Welcome, %s! 🎉</h2>
            <p>You’ve successfully registered your UniClub account.</p>
            <p>👉 Log in here: <a href="https://uniclub.id.vn/login">https://uniclub.id.vn/login</a></p>
        """.formatted(fullName);

        sendEmail(to, "[UniClub] Welcome to the system 🎉", content);
    }

    @Override
    public void sendResetPasswordEmail(String to, String fullName, String link) {
        String content = """
            <h2 style="color:#1E88E5;">Reset Your Password</h2>
            <p>Hi %s,</p>
            <p>You requested to reset your password.</p>
            <a href="%s" style="display:inline-block;padding:12px 24px;
                background-color:#1E88E5;color:white;border-radius:6px;text-decoration:none;">
                Reset Password
            </a>
            <p style="margin-top:12px;">The link expires in 15 minutes.</p>
        """.formatted(fullName, link);

        sendEmail(to, "Reset your UniClub password", content);
    }

    @Override
    public void sendClubApplicationRejectedEmail(String to, String clubName, String reason) {
        String content = """
            <h2 style="color:#D32F2F;">Club Creation Rejected ❌</h2>
            <p>The request to establish <b>%s</b> has been rejected.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(clubName, reason);

        sendEmail(to, "Club creation request rejected", content);
    }

    @Override
    public void sendClubApplicationApprovedEmail(String to, String fullName, String clubName) {
        String content = """
            <h2 style="color:#1E88E5;">Club Approved 🎉</h2>
            <p>Hello <b>%s</b>,</p>
            <p>Your club creation request for <b>%s</b> has been approved!</p>
            <p>Two official accounts will be created by UniStaff soon.</p>
        """.formatted(fullName, clubName);

        sendEmail(to, "Club creation request approved", content);
    }

    @Override
    public void sendClubCreationCompletedEmail(
            String to,
            String proposerName,
            String clubName,
            String leaderName,
            String leaderEmail,
            String viceName,
            String viceEmail,
            String defaultPassword
    ) {
        String content = """
            <h2 style="color:#1E88E5;">Your Club Has Been Created 🎉</h2>
            <p>Hello <b>%s</b>,</p>
            <p>The club <b>%s</b> has been successfully created.</p>

            <h3>Account Information</h3>
            <p><b>Leader:</b> %s (%s)</p>
            <p><b>Vice Leader:</b> %s (%s)</p>
            <p><b>Default password:</b> %s</p>

            <p>You may log in at: <a href='https://uniclub.id.vn/login'>uniclub.id.vn/login</a></p>
        """.formatted(
                proposerName, clubName,
                leaderName, leaderEmail,
                viceName, viceEmail,
                defaultPassword
        );

        sendEmail(to, "[UniClub] Club Created Successfully", content);
    }

    @Override
    public void sendEventRegistrationEmail(String to, String fullName, Event event, long commitPoints) {

        // 🔹 Build date text cho multi-day
        String dateText = "TBA";
        if (event.getStartDate() != null && event.getEndDate() != null) {
            if (event.getStartDate().isEqual(event.getEndDate())) {
                // Event 1 ngày
                dateText = event.getStartDate().toString();
            } else {
                // Event nhiều ngày
                dateText = event.getStartDate() + " - " + event.getEndDate();
            }
        }

        // 🔹 Location safe null
        String locationName = (event.getLocation() != null)
                ? event.getLocation().getName()
                : "To be announced";

        String content = """
        <h2 style="color:#1E88E5;">Event Registration Confirmed 🎉</h2>
        <p>Hello %s,</p>
        <p>You successfully registered for <b>%s</b>.</p>
        <p>
           <b>Date:</b> %s<br>
           <b>Location:</b> %s<br>
           <b>Commitment Points Locked:</b> %d
        </p>
        """.formatted(
                fullName,
                event.getName(),
                dateText,
                locationName,
                commitPoints
        );

        sendEmail(to, "[UniClub] Event Registration Confirmation", content);
    }


    @Override
    public void sendEventSummaryEmail(
            String to,
            String fullName,
            Event event,
            long rewardPoints,
            String feedbackLink
    ) {
        String content = """
            <h2 style="color:#1E88E5;">Event Summary 🎉</h2>
            <p>Hello %s,</p>
            <p>Thank you for attending <b>%s</b>.</p>
            <p>You earned <b>%d points</b>.</p>

            <a href="%s" style="display:inline-block;padding:12px 24px;
                background:#1976D2;color:white;border-radius:6px;text-decoration:none;">
                Submit Feedback
            </a>
        """.formatted(fullName, event.getName(), rewardPoints, feedbackLink);

        sendEmail(to, "[UniClub] Event Summary & Feedback Request", content);
    }

    @Override
    public void sendEventCancellationEmail(String to, String fullName, Event event, long refundPoints) {

        // ⚠ refundPoints giờ luôn = 0 khi user hủy đăng ký
        String content = """
        <h2 style="color:#D32F2F;">Registration Cancelled ❌</h2>

        <p>Hello <b>%s</b>,</p>

        <p>You have cancelled your registration for the event <b>%s</b>.</p>

        <div style="
            margin: 20px 0;
            padding: 18px;
            background: #FFF4F4;
            border-left: 4px solid #D32F2F;
            border-radius: 8px;">
            <p style="margin:0; color:#B71C1C;">
                ⚠ <b>Your commitment points will <u>not</u> be refunded.</b>
            </p>
        </div>

        <p>If this was a mistake, please contact your club management team.</p>
    """.formatted(fullName, event.getName());

        sendEmail(to, "[UniClub] Registration Cancelled – No Refund", content);
    }


    @Override
    public void sendSuspiciousAttendanceEmail(String to, String fullName, Event event) {
        String content = """
            <h2 style="color:#D32F2F;">Suspicious Attendance Detected ⚠</h2>
            <p>Hello %s,</p>
            <p>Your attendance for <b>%s</b> has been marked as suspicious.</p>
            <p>Please contact club leaders if this is incorrect.</p>
        """.formatted(fullName, event.getName());

        sendEmail(to, "[UniClub] Attendance Marked as Suspicious", content);
    }

    @Override
    public void sendEventStaffAssignmentEmail(String to, String fullName, Event event, String duty) {

        String dateText;
        if (event.getStartDate() != null && event.getEndDate() != null) {
            if (event.getStartDate().equals(event.getEndDate())) {
                dateText = event.getStartDate().toString();
            } else {
                dateText = event.getStartDate() + " - " + event.getEndDate();
            }
        } else {
            dateText = "To be announced";
        }

        String location = (event.getLocation() != null)
                ? event.getLocation().getName()
                : "Unknown";

        String content = """
        <h2 style="color:#1E88E5;">You Have Been Assigned as Event Staff 🎉</h2>
        <p>Hello <b>%s</b>,</p>
        <p>You are assigned as <b>%s</b> for event <b>%s</b>.</p>
        <p>Date: %s<br>Location: %s</p>
    """.formatted(fullName, duty, event.getName(), dateText, location);

        sendEmail(to, "[UniClub] Event Staff Assignment", content);
    }


    @Override
    public void sendMemberApplicationSubmitted(String to, String fullName, String clubName) {
        String content = """
            <h2 style="color:#1E88E5;">Membership Application Submitted</h2>
            <p>Hi %s,</p>
            <p>Your request to join <b>%s</b> has been submitted.</p>
        """.formatted(fullName, clubName);

        sendEmail(to, "Your membership application has been submitted", content);
    }

    @Override
    public void sendMemberApplicationResult(
            String to, String fullName, String clubName, boolean approved
    ) {
        String content = approved
                ? """
                    <h2 style="color:green;">Application Approved 🎉</h2>
                    <p>Congratulations %s,</p>
                    <p>You are now a member of <b>%s</b>.</p>
                """.formatted(fullName, clubName)
                : """
                    <h2 style="color:#D32F2F;">Application Rejected ❌</h2>
                    <p>Hi %s,</p>
                    <p>Your request to join <b>%s</b> has been rejected.</p>
                """.formatted(fullName, clubName);

        sendEmail(to, approved
                ? "Your application to " + clubName + " is approved!"
                : "Your application to " + clubName + " has been updated", content);
    }

    @Override
    public void sendMemberApplicationCancelled(String to, String fullName, String clubName) {
        String content = """
            <h2 style="color:#D32F2F;">Application Cancelled</h2>
            <p>Hi %s,</p>
            <p>Your application to join <b>%s</b> has been cancelled.</p>
        """.formatted(fullName, clubName);

        sendEmail(to, "Membership application cancelled", content);
    }

    @Override
    public void sendClubNewMembershipRequestEmail(
            String to, String leaderName, String clubName, String applicantName
    ) {
        String content = """
            <h2 style="color:#1E88E5;">New Membership Request</h2>
            <p>Dear %s,</p>
            <p>A new user (<b>%s</b>) wants to join <b>%s</b>.</p>
        """.formatted(leaderName, applicantName, clubName);

        sendEmail(to, "[UniClub] New membership request for " + clubName, content);
    }

    @Override
    public void sendMemberKickedEmail(String to, String fullName, String clubName,
                                      String approverName, String reason) {

        String content = """
        <h2 style="color:#d9534f;">Club Membership Update</h2>
        <p>Dear <b>%s</b>,</p>
        <p>You have been removed from the club <b>%s</b>.</p>
        <p><b>Reason:</b> %s</p>
        <p><b>Action taken by:</b> %s</p>
    """.formatted(fullName, clubName, reason, approverName);

        sendEmail(to, "[UniClub] You have been removed from " + clubName, content);
    }



    @Override
    public void sendLeaveRequestSubmittedToLeader(
            String to, String leaderName, String memberName,
            String clubName, String reason
    ) {
        String content = """
            <h2 style="color:#1E88E5;">Leave Request Submitted</h2>
            <p>Dear %s,</p>
            <p><b>%s</b> has requested to leave <b>%s</b>.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(leaderName, memberName, clubName,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason);

        sendEmail(to, "[UniClub] Member submitted a club leave request", content);
    }

    @Override
    public void sendLeaveRequestApprovedToMember(String to, String memberName, String clubName) {
        String content = """
            <h2 style="color:green;">Leave Request Approved</h2>
            <p>Dear %s,</p>
            <p>Your request to leave <b>%s</b> has been approved.</p>
        """.formatted(memberName, clubName);

        sendEmail(to, "[UniClub] Your club leave request has been approved", content);
    }

    @Override
    public void sendLeaveRequestRejectedToMember(
            String to, String memberName, String clubName, String leaderName
    ) {
        String content = """
            <h2 style="color:#D32F2F;">Leave Request Rejected</h2>
            <p>Dear %s,</p>
            <p>Your request to leave <b>%s</b> was rejected by <b>%s</b>.</p>
        """.formatted(memberName, clubName, leaderName);

        sendEmail(to, "[UniClub] Your club leave request has been rejected", content);
    }

    @Override
    public void sendPointRequestSubmittedToStaff(
            String to, String staffName, String clubName, long points, String reason
    ) {
        String content = """
            <h2 style="color:#1E88E5;">New Point Request</h2>
            <p>Dear %s,</p>
            <p>Club <b>%s</b> submitted a point request.</p>
            <p><b>Points:</b> %d</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(staffName, clubName, points,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason);

        sendEmail(to, "[UniClub] New Point Request Submitted", content);
    }

    @Override
    public void sendPointRequestApproved(
            String to, String fullName, String clubName, long points, String note
    ) {
        String content = """
            <h2 style="color:green;">Point Request Approved</h2>
            <p>Dear %s,</p>
            <p>Your request for <b>%s</b> has been approved.</p>
            <p><b>Points Added:</b> %d</p>
            <p><b>Staff Note:</b> %s</p>
        """.formatted(fullName, clubName, points,
                (note == null || note.isBlank()) ? "No note provided" : note);

        sendEmail(to, "[UniClub] Your Point Request Has Been Approved", content);
    }

    @Override
    public void sendPointRequestRejected(
            String to, String fullName, String clubName, String note
    ) {
        String content = """
            <h2 style="color:#D32F2F;">Point Request Rejected</h2>
            <p>Dear %s,</p>
            <p>Your request for <b>%s</b> has been rejected.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(fullName, clubName,
                (note == null || note.isBlank()) ? "No reason provided" : note);

        sendEmail(to, "[UniClub] Your Point Request Has Been Rejected", content);
    }

    @Override
    public void sendClubRedeemEmail(String to, String fullName, String productName,
                                    int quantity, long totalPoints, String orderCode, String qrUrl) {

        String content = """
            <h2 style="color:#1E88E5;">Redeem Successful 🎁</h2>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity:</b> %d</p>
            <p><b>Points Spent:</b> %d</p>
            <p><b>Order Code:</b> %s</p>
            <div style='text-align:center;margin:20px'>
                <img src="%s" style="width:150px">
            </div>
        """.formatted(productName, quantity, totalPoints, orderCode, qrUrl);

        sendEmail(to, "[UniClub] Redemption Confirmation #" + orderCode, content);
    }

    @Override
    public void sendEventRedeemEmail(String to, String fullName, String eventName,
                                     String productName, int quantity, long totalPoints,
                                     String orderCode, String qrUrl) {

        String content = """
            <h2 style="color:#1E88E5;">Event Gift Redeemed 🎉</h2>
            <p><b>Event:</b> %s</p>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity:</b> %d</p>
            <p><b>Points Spent:</b> %d</p>
            <p><b>Order Code:</b> %s</p>
            <div style='text-align:center;margin:20px'>
                <img src="%s" style="width:150px">
            </div>
        """.formatted(eventName, productName, quantity, totalPoints, orderCode, qrUrl);

        sendEmail(to, "[UniClub] Event Gift Redemption – " + eventName, content);
    }

    @Override
    public void sendRefundEmail(String to, String fullName, String productName,
                                int quantity, long refundPoints, String reason,
                                String orderCode) {

        String content = """
            <h2 style="color:green;">Refund Successful</h2>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity:</b> %d</p>
            <p><b>Points Refunded:</b> %d</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(productName, quantity, refundPoints, reason);

        sendEmail(to, "[UniClub] Refund Successful for Order #" + orderCode, content);
    }

    @Override
    public void sendPartialRefundEmail(String to, String fullName, String productName,
                                       int quantityRefunded, long refundPoints, String reason,
                                       String orderCode) {

        String content = """
            <h2 style="color:#1E88E5;">Partial Refund Processed</h2>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity Refunded:</b> %d</p>
            <p><b>Points Refunded:</b> %d</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(productName, quantityRefunded, refundPoints, reason);

        sendEmail(to, "[UniClub] Partial Refund for Order #" + orderCode, content);
    }

    @Override
    public void sendCheckInRewardEmail(String to, String fullName, String eventName,
                                       long pointsEarned, long totalPoints) {

        String content = """
            <h2 style="color:#1E88E5;">Reward Received 🎉</h2>
            <p>Hello <b>%s</b>,</p>
            <p>You earned <b>%d points</b> from attending <b>%s</b>.</p>
            <p>Your new balance: <b>%d points</b>.</p>
        """.formatted(fullName, pointsEarned, eventName, totalPoints);

        sendEmail(to, "[UniClub] Event Reward Points Received", content);
    }

    @Override
    public void sendManualBonusEmail(String to, String fullName,
                                     long bonusPoints, String reason, long totalPoints) {

        String content = """
            <h2 style="color:#1E88E5;">Bonus Points Awarded 🎁</h2>
            <p>Hello <b>%s</b>,</p>
            <p>You received <b>%d bonus points</b>.</p>
            <p><b>Reason:</b> %s</p>
            <p>Your new balance: <b>%d points</b>.</p>
        """.formatted(fullName, bonusPoints,
                (reason == null || reason.isBlank()) ? "No specific reason" : reason,
                totalPoints);

        sendEmail(to, "[UniClub] You Received Bonus Points", content);
    }

    @Override
    public void sendMilestoneEmail(String to, String fullName, long milestone) {

        String content = """
            <h2 style="color:#1E88E5;">Milestone Reached 🏆</h2>
            <p>Hello <b>%s</b>,</p>
            <p>You reached <b>%d UniPoints</b>!</p>
            <p>Keep participating to earn more!</p>
        """.formatted(fullName, milestone);

        sendEmail(to, "[UniClub] Milestone Achievement Reached", content);
    }

    @Override
    public void sendClubTopUpEmail(String to, String fullName,
                                   String clubName, long points, String reason) {

        String content = """
            <h2 style="color:green;">Club Wallet Credited 💰</h2>
            <p>Hello <b>%s</b>,</p>
            <p>Club <b>%s</b> has been credited <b>%d points</b>.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(
                fullName, clubName, points,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, "[UniClub] Club Wallet Credited", content);
    }

    @Override
    public void sendClubWalletDeductionEmail(String to, String fullName,
                                             String clubName, long points, String reason) {

        String content = """
            <h2 style="color:#D32F2F;">Club Wallet Deduction ⚠</h2>
            <p>Hello <b>%s</b>,</p>
            <p>Club <b>%s</b> spent <b>%d points</b>.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(
                fullName, clubName, points,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, "[UniClub] Club Wallet Deducted", content);
    }

    @Override
    public void sendClubBatchDeductionSummaryEmail(String to, String fullName,
                                                   String clubName, long totalPoints,
                                                   int memberCount, String reason) {

        String content = """
            <h2 style="color:#1E88E5;">Batch Distribution Summary 📢</h2>
            <p>Hello <b>%s</b>,</p>
            <p>Club <b>%s</b> distributed <b>%d points</b> to <b>%d members</b>.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(
                fullName, clubName, totalPoints, memberCount,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, "[UniClub] Batch Points Distributed", content);
    }

    @Override
    public void sendPublicEventCheckinEmail(
            String to,
            String fullName,
            String eventName,
            LocalTime startTime,
            String location
    ) {

        String formattedTime = (startTime != null) ? startTime.toString() : "Not specified";

        String content = """
            <h2 style="color:#1E88E5;">Check-in Successful 🎉</h2>
            <p>Hello <b>%s</b>,</p>
            <p>You checked in to <b>%s</b>.</p>
            <p><b>Time:</b> %s<br><b>Location:</b> %s</p>
        """.formatted(fullName, eventName, formattedTime,
                location != null ? location : "Not specified");

        sendEmail(to, "Successfully Checked In: " + eventName, content);
    }

    @Override
    public void sendUpcomingEventReminderEmail(
            String to,
            String fullName,
            String eventName,
            String eventTime,
            String location
    ) {

        String content = """
            <h2 style="color:#1E88E5;">Event Reminder ⏰</h2>
            <p>Hello <b>%s</b>,</p>
            <p>Your event <b>%s</b> is happening soon.</p>
            <p><b>Time:</b> %s<br><b>Location:</b> %s</p>
        """.formatted(fullName, eventName,
                eventTime != null ? eventTime : "Not specified",
                location != null ? location : "Not specified");

        sendEmail(to, "Reminder: Upcoming Event - " + eventName, content);
    }

    @Override
    public void sendEventAwaitingUniStaffReviewEmail(
            String to,
            String eventName,
            String eventDate
    ) {

        String content = """
            <h2 style="color:#1E88E5;">Event Pending UniStaff Review</h2>
            <p>The event <b>%s</b> is awaiting approval.</p>
            <p><b>Date:</b> %s</p>
        """.formatted(eventName, eventDate);

        sendEmail(to, "[UniClub] Event awaiting UniStaff review", content);
    }

    @Override
    public void sendCoHostInviteEmail(String to, String clubName, String eventName) {
        String content = """
            <h2 style="color:#1E88E5;">Co-host Invitation 🤝</h2>
            <p>Your club <b>%s</b> was invited to co-host:</p>
            <p><b>%s</b></p>
        """.formatted(clubName, eventName);

        sendEmail(to, "[UniClub] You are invited to co-host an event", content);
    }

    @Override
    public void sendEventWaitingUniStaffEmail(String to, String eventName) {
        String content = """
            <h2 style="color:#1E88E5;">Awaiting Co-Host Responses ⏳</h2>
            <p>The event <b>%s</b> is waiting for co-host responses.</p>
        """.formatted(eventName);

        sendEmail(to, "[UniClub] Event waiting for co-host responses", content);
    }

    @Override
    public void sendHostEventRejectedByCoHostEmail(String to, String eventName, String rejectedClubName) {
        String content = """
            <h2 style="color:#D32F2F;">Co-host Rejected Event ❌</h2>
            <p>The co-host <b>%s</b> rejected your event:</p>
            <p><b>%s</b></p>
        """.formatted(rejectedClubName, eventName);

        sendEmail(to, "[UniClub] Co-host rejected your event", content);
    }

    @Override
    public void sendEventApprovedEmail(String to, String eventName, long approvedPoints) {
        String content = """
            <h2 style="color:green;">Event Approved 🎉</h2>
            <p>Your event <b>%s</b> was approved.</p>
            <p><b>Budget:</b> %d points</p>
        """.formatted(eventName, approvedPoints);

        sendEmail(to, "[UniClub] Your event has been approved", content);
    }

    @Override
    public void sendEventRejectedEmail(String to, String eventName, String reason, String staffName) {
        String content = """
            <h2 style="color:#D32F2F;">Event Rejected ❌</h2>
            <p>Your event <b>%s</b> was rejected by <b>%s</b>.</p>
            <p><b>Reason:</b> %s</p>
        """.formatted(eventName, staffName, reason);

        sendEmail(to, "[UniClub] Event Rejected", content);
    }

    @Override
    public void sendPenaltyNotificationEmail(User user, Club club, PenaltyRule rule, ClubPenalty penalty) {

        String content = """
            <h2 style="color:#D84315;">⚠ Penalty Notification</h2>
            <p>Hello <b>%s</b>,</p>
            <p>You received a penalty from <b>%s</b>.</p>

            <div style="
                border:1px solid #ddd;
                background:#FAFAFA;
                padding:18px;
                border-radius:10px;
                margin-top:16px;">

                <p><b>📌 Violation:</b> %s</p>
                <p><b>🏷 Reason:</b> %s</p>
                <p><b>➖ Points Deducted:</b> <span style="color:#D32F2F;">-%d</span></p>
                <p><b>👤 Issued by:</b> %s</p>
                <p><b>🕒 Issued at:</b> %s</p>
            </div>

            <p style="margin-top:18px;">
                If you believe this penalty was issued in error, contact your club management team.
            </p>
        """.formatted(
                user.getFullName(),
                club.getName(),
                rule.getName(),
                penalty.getReason(),
                rule.getPenaltyPoints(),
                penalty.getCreatedBy().getFullName(),
                penalty.getCreatedAt().toString()
        );

        sendEmail(user.getEmail(),
                "[UniClub] Penalty Notice from " + club.getName(),
                content);
    }

    @Override
    public void sendClubCreationOtpEmail(
            String to,
            String fullName,
            String otpCode,
            String createClubLink
    ) {

        String content = """
    <h2 style="color:#1E88E5;">Club Creation Authorization</h2>

    <p>Hello <b>%s</b>,</p>

    <p>You are permitted to submit a club creation request on UniClub.</p>

    <p><b>👉 Click the button below to start:</b></p>

    <a href="%s" style="
        display:inline-block;
        padding:14px 28px;
        background:#1E88E5;
        color:white;
        border-radius:8px;
        text-decoration:none;
        font-weight:600;
        margin:16px 0;
    ">
        Create Club Application
    </a>

    <p style="margin-top:20px;"><b>Or use this OTP manually:</b></p>

    <div class="otp-box" style="
        font-size: 32px;
        font-weight: bold;
        letter-spacing: 6px;
        text-align: center;
        margin: 16px 0;
        padding: 16px;
        background:#E8F1FF;
        color:#1E88E5;
        border-radius: 10px;
        border: 1px solid #C8DAFF;
    ">
        %s
    </div>

    <p>This authorization is valid for <b>48 hours</b>.</p>
    """.formatted(fullName, createClubLink, otpCode);

        sendEmail(to, "[UniClub] Club Creation Authorization", content);
    }

    @Override
    public void sendEventCancelledEmail(String to, String eventName, String eventDate, String reason) {

        String content = """
        <h2 style="color:#D32F2F;">Event Cancelled ❌</h2>

        <p>Hello,</p>

        <p>The event <b>%s</b> scheduled on <b>%s</b> has been officially <b>cancelled</b>.</p>

        <p style="margin-top:10px;">
            <b>Reason for cancellation:</b><br>
            <span style="color:#D32F2F; font-weight:600;">%s</span>
        </p>

        <p style="margin-top:16px;">
            All related registrations, staff assignments, and scheduled activities have been removed.
        </p>

        <p>If this affects any planning or commitments, please reach out to UniStaff or your club management team.</p>

        <p style="margin-top:22px;">
            Thank you for your understanding.<br>
            <span style="color:#1E88E5; font-weight:600;">UniClub Vietnam</span>
        </p>
    """.formatted(eventName, eventDate, reason);

        sendEmail(to, "[UniClub] Event Cancelled – " + eventName, content);
    }
    @Override
    public void sendNewMembershipRequestToLeader(String to, String leaderName, String clubName, String applicantName) {

        String content = """
        <h2 style="color:#1E88E5;">New Membership Request 🚀</h2>
        <p>Dear %s,</p>

        <p><b>%s</b> has submitted a request to join <b>%s</b>.</p>

        <p>Please log in to your Leader Dashboard to approve or reject this request.</p>
    """.formatted(leaderName, applicantName, clubName);

        sendEmail(to, "[UniClub] New membership request for " + clubName, content);
    }
    @Override
    public void sendMemberRedeemNotifyLeaderEmail(
            String to,
            String leaderName,
            String memberName,
            String studentCode,
            String productName,
            int quantity,
            long totalPoints,
            String orderCode
    ) {

        String content = """
        <h2 style="color:#1E88E5;">Member Redeemed a Reward 🎁</h2>

        <p>Dear <b>%s</b>,</p>

        <p>One of your club members just redeemed an item.</p>

        <div style="
            margin: 20px 0;
            padding: 16px;
            background:#F5F9FF;
            border-left: 4px solid #1E88E5;
            border-radius: 10px;
        ">
            <p><b>👤 Member:</b> %s (%s)</p>
            <p><b>🎁 Product:</b> %s</p>
            <p><b>🔢 Quantity:</b> %d</p>
            <p><b>💰 Points spent:</b> %d</p>
            <p><b>📦 Order Code:</b> %s</p>
        </div>

        <p>You can track order details in your Leader Dashboard.</p>
    """.formatted(leaderName, memberName, studentCode, productName, quantity, totalPoints, orderCode);

        sendEmail(to, "[UniClub] Member Redeemed Reward #" + orderCode, content);
    }
    @Override
    public void sendClubMonthlyLockedEmail(String to, String clubName, int month, int year,  String lockedBy) {

        String content = """
    <h2 style="color:#1E88E5;">Monthly Report Has Been Locked 🔒</h2>

    <p>The report for <b>%d/%d</b> of the club <b>%s</b> has been <b>locked</b> by UniStaff.</p>

    <p>After locking, the club will no longer be able to edit the monthly report, scoring data, or reward points.</p>

    <p>If you require any special adjustments, please contact UniStaff.</p>
    """.formatted(month, year, clubName);

        sendEmail(to, "[UniClub] Monthly Report Locked", content);
    }
    @Override
    public void sendClubMonthlyRewardApprovedEmail(String to, String clubName,
                                                   long rewardPoints, long newBalance,
                                                   int month, int year, String staffName) {

        String content = """
    <h2 style="color:green;">Monthly Reward Approved 🎉</h2>

    <p>The monthly reward for <b>%d/%d</b> of the club <b>%s</b> has been approved by UniStaff <b>%s</b>.</p>

    <div style="
            margin: 20px 0;
            padding: 16px;
            background:#F5F9FF;
            border-left: 4px solid #1E88E5;
            border-radius: 10px;
    ">
        <p><b>💰 Reward Points Granted:</b> %d</p>
        <p><b>🏦 Club Wallet Balance After Approval:</b> %d</p>
    </div>

    <p>The club can use these points for:</p>
    <ul>
        <li>Distributing points to members</li>
        <li>Redeeming items in the Store</li>
        <li>Reserving for next month's events</li>
    </ul>
    """.formatted(month, year, clubName, staffName, rewardPoints, newBalance);

        sendEmail(to, "[UniClub] Monthly Reward Approved", content);
    }
    @Override
    public void sendMemberRewardEmail(
            String to,
            String clubName,
            int month,
            int year,

            int finalScore,
            int attendanceScore,
            int staffScore,
            int totalClubSessions,
            int totalClubPresent,
            String staffEvaluation,

            int totalClubScore,
            int clubRewardPool,

            int rewardPoints,
            int oldBalance,
            int newBalance
    ) {

        double attendanceRate = totalClubSessions == 0 ? 0
                : (totalClubPresent * 100.0 / totalClubSessions);

        String content = """
        <h2 style="color:#1E88E5;">Monthly Reward Received 🎉</h2>

        <p>Hello,</p>

        <p>You received <b>%d reward points</b> from <b>%s</b> 
        for your activity performance in <b>%d/%d</b>.</p>

        <h3>Your Activity Breakdown</h3>

        <ul>
            <li><b>Attendance Score:</b> %d 
                <span style="color:gray;">(%d/%d sessions, %.1f%%)</span>
            </li>
            <li><b>Staff Score:</b> %d 
                <span style="color:gray;">(%s)</span>
            </li>
            <li><b>Final Score:</b> %d 
                <br/><span style="color:gray;">
                    = Attendance (%d) + Staff (%d)
                </span>
            </li>
        </ul>

        <h3>How Your Reward Was Calculated</h3>

        <pre style="background:#F5F9FF; padding:12px; border-radius:8px;">
Your Final Score = %d (attendance) + %d (staff) = %d

Your Reward = (FinalScore / TotalClubScore) × ClubRewardPool
             = (%d / %d) × %d
             = %d points
        </pre>

        <div style="background:#F5F9FF; padding:16px; border-radius:10px;
                    border-left:4px solid #1E88E5; margin-top:16px;">
            <p><b>💰 Previous Balance:</b> %d</p>
            <p><b>💵 Reward Points Added:</b> +%d</p>
            <p><b>🏦 New Balance:</b> %d</p>
        </div>

        <p>Thank you for your contributions this month 🎉</p>
        """.formatted(
                // greeting
                rewardPoints, clubName, month, year,

                // breakdown
                attendanceScore, totalClubPresent, totalClubSessions, attendanceRate,
                staffScore, staffEvaluation,
                finalScore, attendanceScore, staffScore,

                // formula line
                attendanceScore, staffScore, finalScore,
                finalScore, totalClubScore, clubRewardPool, rewardPoints,

                // balance
                oldBalance, rewardPoints, newBalance
        );

        sendEmail(to, "[UniClub] Monthly Reward Received", content);
    }



}
