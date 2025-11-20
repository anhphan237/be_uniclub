package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Event;
import com.example.uniclub.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // ✅ true = multipart mode (for inline images)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            // ✅ Cẩn thận: setFrom có 2 tham số cần encoding chuẩn
            helper.setFrom("uniclub.contacts@gmail.com", "UniClub System");
            helper.setTo(to);
            helper.setSubject(subject);


            String html = String.format("""
<div style="font-family: Arial, sans-serif; background: #F7FBFF; 
            border-radius: 12px; padding: 28px; max-width: 600px; margin: auto;
            box-shadow: 0 0 10px rgba(0,0,0,0.08); color-scheme: light !important;">

    <div style="text-align: center; margin-bottom: 22px;">
        <img src='cid:uniclub-logo' alt='UniClub Logo' style='width: 120px;'>
    </div>

    <div style="font-size: 16px; color: #111111 !important; line-height: 1.6;">
        %s
    </div>

    <hr style="margin: 28px 0; border: none; border-top: 1px solid #e2e2e2;">

    <div style="text-align: center; font-size: 14px; color: #444 !important;">
        <p>Best regards,<br><b>UniClub Vietnam</b><br>Digitalizing Communities 💡</p>
    </div>

</div>
""", content);


            // ✅ Đặt nội dung HTML
            helper.setText(html, true);

            // ✅ Inline logo (đảm bảo file nằm đúng đường dẫn)
            helper.addInline("uniclub-logo", new ClassPathResource("static/images/logo.png"));

            mailSender.send(message);
            System.out.println(" Email sent successfully to " + to);

        } catch (MessagingException e) {
            System.err.println(" Messaging error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(" Email send failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @Override
    public void sendFeedbackThankYouEmail(String to, String eventName, int rating) {

        String content = String.format("""
        Hello,

        Thank you for participating in the event: <b>%s</b>.
        We’ve received your feedback with a rating of ⭐ %d stars.

        Your opinion is valuable and helps UniClub improve future events!

        Best regards,
        <b>UniClub Vietnam</b>
        """, eventName, rating);

        sendEmail(to, "Thank You for Your Feedback!", content);
    }
    @Override
    public void sendWelcomeEmail(String to, String fullName) {
        String content = """
            <h2>Hello %s,</h2>
            <p>Congratulations! You’ve successfully registered your <b>UniClub</b> account. 🎉</p>
            <p>You can now log in to explore clubs, join events, and start earning points within the system.</p>
            <p>👉 Access here: <a href="https://uniclub.id.vn/login">https://uniclub.id.vn/login</a></p>
            """.formatted(fullName);

        sendEmail(to, "[UniClub] Welcome to the system 🎉", content);
    }


    @Override
    public void sendResetPasswordEmail(String to, String fullName, String link) {
        String content = """
            Hi %s,<br><br>
            We received a request to reset your UniClub password.<br>
            Click the button below to set a new password:<br><br>
            <a href="%s" style="display:inline-block;padding:10px 20px;
            background-color:#1E88E5;color:white;border-radius:6px;text-decoration:none;">
            Reset Password</a><br><br>
            This link will expire in 15 minutes.
            """.formatted(fullName, link);

        sendEmail(to, "Reset your UniClub password", content);
    }
    @Override
    public void sendClubApplicationRejectedEmail(String to, String clubName, String reason) {

        String content = """
            The request to establish the club <b>%s</b> has been rejected.<br>
            <b>Reason:</b> %s<br><br>
            Please review and resubmit if necessary.
            """.formatted(clubName, reason);

        sendEmail(to, "Club creation request rejected", content);
    }



    @Override
    public void sendClubApplicationApprovedEmail(String to, String fullName, String clubName) {

        String content = """
            Hello <b>%s</b>,<br><br>
            Your club creation request for <b>%s</b> has been successfully approved 🎉<br><br>
            The club has now been created in the UniClub system.<br><br>
            Note:<br>
            - The school will manually create 2 accounts (President & Vice President).<br>
            - These accounts will use the domain <b>@uniclub.edu.vn</b> and will be sent to you via email once ready.<br><br>
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
            Hello %s,<br><br>
            The club <b>%s</b> that you proposed has been approved and successfully created! 🎉<br><br>
            Below are the details of your club’s two main accounts:<br><br>

            🔹 <b>President (Leader)</b><br>
            Full name: %s<br>
            Email: %s<br><br>

            🔹 <b>Vice President (Vice Leader)</b><br>
            Full name: %s<br>
            Email: %s<br><br>

            Default password for both accounts: <b>%s</b><br><br>

            Both accounts can log in at:<br>
            <a href='https://uniclub.id.vn/login'>https://uniclub.id.vn/login</a><br><br>

            The status of your club creation request is now: <b>COMPLETED</b><br><br>
            """.formatted(
                proposerName, clubName,
                leaderName, leaderEmail,
                viceName, viceEmail,
                defaultPassword
        );

        sendEmail(to, "[UniClub] Your club " + clubName + " has been successfully created", content);
    }
    @Override
    public void sendEventRegistrationEmail(String to, String fullName, Event event, long commitPoints) {

        String content = """
            Hello %s,<br><br>
            You have successfully registered for the event <b>%s</b> 🎉<br><br>
            
            🔹 Event Date: %s<br>
            🔹 Location: %s<br>
            🔹 Commitment Points Locked: <b>%d</b><br><br>

            Please remember to check-in during the event to earn rewards.<br><br>

            Best regards,<br>
            <b>UniClub Vietnam</b>
            """.formatted(
                fullName,
                event.getName(),
                event.getDate(),
                event.getLocation(),
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
            Hello %s,<br><br>
            Thank you for participating in the event <b>%s</b> 🎉<br><br>

            Based on your attendance:<br>
            🔹 Reward Points Earned: <b>%d pts</b><br><br>

            We would love to hear your feedback!<br>
            Please click below to submit your evaluation:<br><br>

            <a href="%s" style="padding:10px 20px; background:#1976D2; color:white; border-radius:6px; text-decoration:none;">
            Submit Feedback
            </a><br><br>

            Best regards,<br>
            <b>UniClub Vietnam</b>
            """.formatted(
                fullName,
                event.getName(),
                rewardPoints,
                feedbackLink
        );

        sendEmail(to, "[UniClub] Event Summary & Feedback Request", content);
    }
    @Override
    public void sendEventCancellationEmail(String to, String fullName, Event event, long refundPoints) {

        String content = """
        Hello %s,<br><br>
        Your registration for the event <b>%s</b> has been cancelled.<br><br>

        Refund details:<br>
        🔹 Refunded Points: <b>%d pts</b><br><br>

        If this was a mistake, you may re-register if the event is still open.<br><br>

        Best regards,<br>
        <b>UniClub Vietnam</b>
        """.formatted(
                fullName,
                event.getName(),
                refundPoints
        );

        sendEmail(to, "[UniClub] Event Registration Cancelled", content);
    }
    @Override
    public void sendSuspiciousAttendanceEmail(String to, String fullName, Event event) {

        String content = """
        Hello %s,<br><br>
        Your attendance for the event <b>%s</b> has been marked as 
        <span style="color:#D32F2F;"><b>SUSPICIOUS</b></span> due to inconsistencies detected in check-in data.<br><br>

        If you believe this is a mistake, please contact your club leader or UniStaff for verification.<br><br>

        Best regards,<br>
        <b>UniClub Vietnam</b>
        """.formatted(
                fullName,
                event.getName()
        );

        sendEmail(to, "[UniClub] Attendance Marked as Suspicious", content);
    }
    @Override
    public void sendEventStaffAssignmentEmail(String to, String fullName, Event event, String duty) {

        String content = """
        Hello %s,<br><br>
        You have been assigned as <b>%s</b> for the event <b>%s</b> 🎉<br><br>

        🔹 Event Date: %s<br>
        🔹 Location: %s<br>
        🔹 Duty: <b>%s</b><br><br>

        Please check the event details in UniClub to prepare your tasks.<br><br>

        Best regards,<br>
        <b>UniClub Vietnam</b>
        """.formatted(
                fullName,
                duty,
                event.getName(),
                event.getDate(),
                event.getLocation() != null ? event.getLocation().getName() : "Unknown",
                duty
        );

        sendEmail(to, "[UniClub] You Have Been Assigned as Event Staff", content);
    }
    @Override
    public void sendMemberApplicationSubmitted(String to, String fullName, String clubName) {

        String subject = "Your membership application has been submitted";

        String body = """
            <h2>🎉 Membership Application Submitted</h2>
            <p>Hi %s,</p>
            <p>Your application to join <b>%s</b> has been successfully submitted.</p>
            <p>The club leadership team will review your request soon.</p>
            <p>Thank you for your interest!</p>
            """.formatted(fullName, clubName);

        sendEmail(to, subject, body);
    }

    @Override
    public void sendMemberApplicationResult(
            String to,
            String fullName,
            String clubName,
            boolean approved
    ) {

        String subject = approved
                ? "Your application to " + clubName + " is approved!"
                : "Your application to " + clubName + " has been updated";

        String body = approved
                ? """
                <h2>Application Approved</h2>
                <p>Congratulations %s!</p>
                <p>Your membership request for <b>%s</b> has been <b style='color:green;'>approved</b>.</p>
                <p>Welcome to the club! 🎉</p>
                """.formatted(fullName, clubName)
                : """
                <h2>Application Rejected</h2>
                <p>Hi %s,</p>
                <p>Your membership request for <b>%s</b> has been <b style='color:red;'>rejected</b>.</p>
                <p>You may submit another application if you wish.</p>
                """.formatted(fullName, clubName);

        sendEmail(to, subject, body);
    }

    @Override
    public void sendMemberApplicationCancelled(String to, String fullName, String clubName) {

        String subject = "Membership application cancelled";

        String body = """
            <h2>⚠ Application Cancelled</h2>
            <p>Hi %s,</p>
            <p>Your application to join <b>%s</b> has been successfully cancelled.</p>
            <p>If this was a mistake, you can submit a new application anytime.</p>
            """.formatted(fullName, clubName);

        sendEmail(to, subject, body);
    }


    @Override
    public void sendClubNewMembershipRequestEmail(
            String to,
            String leaderName,
            String clubName,
            String applicantName
    ) {
        String subject = "[UniClub] New membership request for " + clubName;

        String body = """
        <p>Dear %s,</p>
        <p>A new member has requested to join your club <b>%s</b>.</p>
        <p><b>Applicant:</b> %s</p>
        <p>Please log in to UniClub to review and approve/reject this request.</p>
        <br>
        <p>Regards,<br><b>UniClub System</b></p>
        """.formatted(leaderName, clubName, applicantName);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendMemberKickedEmail(
            String to,
            String memberName,
            String clubName,
            String kickerName
    ) {

        String subject = "You have been removed from " + clubName;

        String body = """
        <p>Dear %s,</p>
        <p>You have been <b style='color:red;'>removed</b> from the club <b>%s</b> by <b>%s</b>.</p>
        <p>If you believe this was a mistake, please contact your Club Leader or University Staff.</p>
        <br>
        <p>Best regards,<br><b>UniClub System</b></p>
        """.formatted(memberName, clubName, kickerName);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendLeaveRequestSubmittedToLeader(
            String to,
            String leaderName,
            String memberName,
            String clubName,
            String reason
    ) {
        String subject = "[UniClub] Member submitted a club leave request";

        String body = """
        <p>Dear %s,</p>
        <p>Member <b>%s</b> has submitted a request to leave the club <b>%s</b>.</p>
        <p><b>Reason:</b> %s</p>
        <p>Please log in to the UniClub platform to review this request.</p>
        <br>
        <p>Regards,<br><b>UniClub System</b></p>
        """.formatted(
                leaderName,
                memberName,
                clubName,
                (reason == null || reason.isBlank()) ? "No reason provided." : reason
        );

        sendEmail(to, subject, body);
    }
    @Override
    public void sendLeaveRequestApprovedToMember(
            String to,
            String memberName,
            String clubName
    ) {
        String subject = "[UniClub] Your club leave request has been approved";

        String body = """
        <p>Dear %s,</p>
        <p>Your request to leave the club <b>%s</b> has been <b style='color:green;'>approved</b>.</p>
        <p>You are no longer a member of this club.</p>
        <br>
        <p>Thank you for being part of UniClub,<br><b>UniClub Team</b></p>
        """.formatted(memberName, clubName);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendLeaveRequestRejectedToMember(
            String to,
            String memberName,
            String clubName,
            String leaderName
    ) {
        String subject = "[UniClub] Your club leave request has been rejected";

        String body = """
        <p>Dear %s,</p>
        <p>Your request to leave the club <b>%s</b> has been <b style='color:red;'>rejected</b> by Leader <b>%s</b>.</p>
        <p>Please contact your Club Leader if you need more clarification.</p>
        <br>
        <p>Regards,<br><b>UniClub Team</b></p>
        """.formatted(memberName, clubName, leaderName);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendPointRequestSubmittedToStaff(
            String to,
            String staffName,
            String clubName,
            long points,
            String reason
    ) {
        String subject = "[UniClub] New Point Request Submitted by " + clubName;

        String content = """
        <p>Dear %s,</p>
        <p>The club <b>%s</b> has submitted a new <b>Point Request</b>.</p>
        
        <p><b>Requested Points:</b> %d</p>
        <p><b>Reason:</b> %s</p>

        <p>Please log in to the UniClub dashboard to review and approve/reject this request.</p>

        <br>
        <p>Best regards,<br><b>UniClub System</b></p>
        """.formatted(
                staffName,
                clubName,
                points,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, subject, content);
    }
    @Override
    public void sendPointRequestApproved(
            String to,
            String fullName,
            String clubName,
            long points,
            String note
    ) {
        String subject = "[UniClub] Your Point Request Has Been Approved";

        String content = """
        <p>Dear %s,</p>
        <p>Your point request for <b>%s</b> has been <span style='color:green;'><b>APPROVED</b></span>.</p>
        
        <p><b>Approved Points:</b> %d</p>
        <p><b>Staff Note:</b> %s</p>

        <br>
        <p>The approved points have already been added to your club wallet.</p>
        <p>Best regards,<br><b>UniClub System</b></p>
        """.formatted(
                fullName,
                clubName,
                points,
                (note == null || note.isBlank()) ? "No note provided" : note
        );

        sendEmail(to, subject, content);
    }
    @Override
    public void sendPointRequestRejected(
            String to,
            String fullName,
            String clubName,
            String note
    ) {
        String subject = "[UniClub] Your Point Request Has Been Rejected";

        String content = """
        <p>Dear %s,</p>
        <p>Your point request for <b>%s</b> has been <span style='color:red;'><b>REJECTED</b></span>.</p>

        <p><b>Staff Note:</b> %s</p>

        <br>
        <p>If you believe this was a mistake, please contact the University Staff for clarification.</p>
        <p>Best regards,<br><b>UniClub System</b></p>
        """.formatted(
                fullName,
                clubName,
                (note == null || note.isBlank()) ? "No reason provided" : note
        );

        sendEmail(to, subject, content);
    }
    @Override
    public void sendClubRedeemEmail(String to, String fullName, String productName,
                                    int quantity, long totalPoints,
                                    String orderCode, String qrUrl) {

        String subject = "[UniClub] Redemption Confirmation #" + orderCode;

        String body = """
            <h3>You have successfully redeemed your product!</h3>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity:</b> %d</p>
            <p><b>Points deducted:</b> %d</p>
            <p><b>Order code:</b> %s</p>
            <div style='text-align:center;margin:20px 0'>
                <img src="%s" alt="QR Code" style="width:150px"/>
            </div>
            """.formatted(productName, quantity, totalPoints, orderCode, qrUrl);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendEventRedeemEmail(String to, String fullName, String eventName,
                                     String productName, int quantity, long totalPoints,
                                     String orderCode, String qrUrl) {

        String subject = "[UniClub] Event Gift Redemption – " + eventName;

        String body = """
            <h3>Gift redemption at the event was successful!</h3>
            <p><b>Event:</b> %s</p>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity:</b> %d</p>
            <p><b>Points deducted:</b> %d</p>
            <p><b>Order code:</b> %s</p>
            <div style='text-align:center;margin:20px 0'>
                <img src="%s" alt="QR Code" style="width:150px"/>
            </div>
            """.formatted(eventName, productName, quantity, totalPoints, orderCode, qrUrl);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendRefundEmail(String to, String fullName, String productName,
                                int quantity, long refundPoints, String reason,
                                String orderCode) {

        String subject = "[UniClub] Refund Successful for Order #" + orderCode;

        String body = """
            <h3>Points refund successful!</h3>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity refunded:</b> %d</p>
            <p><b>Points refunded:</b> %d</p>
            <p><b>Reason:</b> %s</p>
            """.formatted(productName, quantity, refundPoints, reason);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendPartialRefundEmail(String to, String fullName, String productName,
                                       int quantityRefunded, long refundPoints, String reason,
                                       String orderCode) {

        String subject = "[UniClub] Partial Refund for Order #" + orderCode;

        String body = """
            <h3>Partial points refund!</h3>
            <p><b>Product:</b> %s</p>
            <p><b>Quantity refunded:</b> %d</p>
            <p><b>Points refunded:</b> %d</p>
            <p><b>Reason:</b> %s</p>
            """.formatted(productName, quantityRefunded, refundPoints, reason);

        sendEmail(to, subject, body);
    }
    @Override
    public void sendCheckInRewardEmail(String to, String fullName, String eventName,
                                       long pointsEarned, long totalPoints) {

        String body = """
        <h3>🎉 You received event reward points!</h3>
        <p>Hello <b>%s</b>,</p>
        <p>Thank you for attending <b>%s</b>.</p>
        <p>You have earned <b>%d points</b>.</p>
        <p>Your new total balance is <b>%d points</b>.</p>
    """.formatted(fullName, eventName, pointsEarned, totalPoints);

        sendEmail(to, "[UniClub] Event Reward Points Received", body);
    }
    @Override
    public void sendManualBonusEmail(String to, String fullName,
                                     long bonusPoints, String reason, long totalPoints) {

        String body = """
        <h3>🎁 Bonus Points Awarded</h3>
        <p>Hello <b>%s</b>,</p>
        <p>You have received <b>%d bonus points</b>.</p>
        <p><b>Reason:</b> %s</p>
        <p>Your updated balance is <b>%d points</b>.</p>
    """.formatted(
                fullName,
                bonusPoints,
                (reason == null || reason.isBlank()) ? "No specific reason" : reason,
                totalPoints
        );

        sendEmail(to, "[UniClub] You Received Bonus Points", body);
    }

    @Override
    public void sendMilestoneEmail(String to, String fullName, long milestone) {

        String body = """
        <h3>🏆 Congratulations on Your Achievement!</h3>
        <p>Hello <b>%s</b>,</p>
        <p>You have reached <b>%d UniPoints</b> — an important milestone 🎉</p>
        <p>Keep participating in clubs & events to earn more!</p>
    """.formatted(fullName, milestone);

        sendEmail(to, "[UniClub] Milestone Achievement Reached", body);
    }
    @Override
    public void sendClubTopUpEmail(String to, String fullName,
                                   String clubName, long points, String reason) {

        String body = """
        <h3>💰 Club Wallet Received Points</h3>
        <p>Hello <b>%s</b>,</p>
        <p>Your club <b>%s</b> has been credited with <b>%d points</b>.</p>
        <p><b>Reason:</b> %s</p>
    """.formatted(
                fullName,
                clubName,
                points,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, "[UniClub] Club Wallet Credited", body);
    }
    @Override
    public void sendClubWalletDeductionEmail(String to, String fullName,
                                             String clubName, long points, String reason) {

        String body = """
        <h3>⚠ Club Wallet Deduction</h3>
        <p>Hello <b>%s</b>,</p>
        <p>Your club <b>%s</b> has spent <b>%d points</b>.</p>
        <p><b>Reason:</b> %s</p>
        <p>If this was not you, please contact University Staff immediately.</p>
    """.formatted(
                fullName,
                clubName,
                points,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, "[UniClub] Club Wallet Deducted", body);
    }
    @Override
    public void sendClubBatchDeductionSummaryEmail(String to, String fullName,
                                                   String clubName, long totalPoints,
                                                   int memberCount, String reason) {

        String body = """
        <h3>📢 Batch Reward Deduction Summary</h3>
        <p>Hello <b>%s</b>,</p>
        <p>Your club <b>%s</b> has distributed <b>%d points</b> to <b>%d members</b>.</p>
        <p><b>Reason:</b> %s</p>
        <p>You can view the full transaction list in your Club Wallet.</p>
    """.formatted(
                fullName,
                clubName,
                totalPoints,
                memberCount,
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );

        sendEmail(to, "[UniClub] Batch Points Distributed", body);
    }
    @Override
    public void sendMemberRewardEmail(String to, String fullName,
                                      long points, String reason, long totalBalance) {

        String html = """
        <h2>🎉 Reward Received</h2>
        <p>Hello <b>%s</b>,</p>
        <p>You have received <b>%d UniPoints</b>.</p>
        <p><b>Reason:</b> %s</p>
        <p>Your new total balance is: <b>%d pts</b></p>
        <br>
        <p>Best regards,<br><b>UniClub Vietnam</b></p>
        """.formatted(fullName, points, reason, totalBalance);

        sendEmail(to, "[UniClub] You received UniPoints!", html);
    }

}