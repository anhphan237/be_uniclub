package com.example.uniclub.service;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.EventCoClub;
import com.example.uniclub.entity.User;

import java.time.LocalDateTime;
import java.time.LocalTime;

public interface EmailService {

    void sendEmail(String to, String subject, String htmlBody);

    void sendFeedbackThankYouEmail(String to, String eventName, int rating);

    void sendWelcomeEmail(String to, String fullName);

    void sendResetPasswordEmail(String to, String fullName, String link);

    void sendClubApplicationRejectedEmail(String to, String clubName, String reason);

    void sendClubApplicationApprovedEmail(String to, String fullName, String clubName);

    void sendClubCreationCompletedEmail(
            String to,
            String proposerName,
            String clubName,
            String leaderName,
            String leaderEmail,
            String viceName,
            String viceEmail,
            String defaultPassword
    );
    void sendEventRegistrationEmail(String to, String fullName, Event event, long commitPoints);

    void sendEventSummaryEmail(
            String to,
            String fullName,
            Event event,
            long rewardPoints,
            String feedbackLink
    );
    void sendEventCancellationEmail(String to, String fullName, Event event, long refundPoints);

    void sendSuspiciousAttendanceEmail(String to, String fullName, Event event);

    void sendEventStaffAssignmentEmail(String to, String fullName, Event event, String duty);

    void sendMemberApplicationSubmitted(String to, String fullName, String clubName);

    void sendMemberApplicationResult(
            String to,
            String fullName,
            String clubName,
            boolean approved
    );

    void sendMemberApplicationCancelled(String to, String fullName, String clubName);

    void sendClubNewMembershipRequestEmail(String to, String leaderName, String clubName, String applicantName);

    void sendMemberKickedEmail(String to, String memberName, String clubName, String kickerName);

    void sendLeaveRequestSubmittedToLeader(String to, String leaderName, String memberName, String clubName, String reason);

    void sendLeaveRequestApprovedToMember(String to, String memberName, String clubName);
    void sendLeaveRequestRejectedToMember(String to, String memberName, String clubName, String leaderName);
    void sendPointRequestSubmittedToStaff(String to, String fullName, String clubName, long points, String reason);

    void sendPointRequestApproved(String to, String fullName, String clubName, long points, String note);

    void sendPointRequestRejected(String to, String fullName, String clubName, String note);

    void sendClubRedeemEmail(String to, String fullName, String productName,
                             int quantity, long totalPoints, String orderCode, String qrUrl);

    void sendEventRedeemEmail(String to, String fullName, String eventName,
                              String productName, int quantity, long totalPoints,
                              String orderCode, String qrUrl);

    void sendRefundEmail(String to, String fullName, String productName,
                         int quantity, long refundPoints, String reason,
                         String orderCode);

    void sendPartialRefundEmail(String to, String fullName, String productName,
                                int quantityRefunded, long refundPoints, String reason,
                                String orderCode);

    void sendCheckInRewardEmail(String to, String fullName, String eventName,
                                long pointsEarned, long totalPoints);

    void sendManualBonusEmail(String to, String fullName, long bonusPoints,
                              String reason, long totalPoints);

    void sendMilestoneEmail(String to, String fullName, long milestone);

    void sendClubTopUpEmail(String to, String fullName, String clubName,
                            long points, String reason);

    void sendClubWalletDeductionEmail(String to, String fullName, String clubName,
                                      long points, String reason);

    void sendClubBatchDeductionSummaryEmail(String to, String fullName, String clubName,
                                            long totalPoints, int memberCount, String reason);


    void sendUpcomingEventReminderEmail(
            String to,
            String fullName,
            String eventName,
            String eventTime,
            String location
    );

    void sendPublicEventCheckinEmail(
            String to,
            String fullName,
            String eventName,
            LocalTime startTime,
            String location
    );

    void sendEventAwaitingUniStaffReviewEmail(
            String to,
            String eventName,
            String eventDate
    );


    void sendCoHostInviteEmail(
            String to,
            String clubName,
            String eventName
    );



    void sendEventWaitingUniStaffEmail(
            String to,
            String eventName
    );
    void sendHostEventRejectedByCoHostEmail(
            String to,
            String eventName,
            String rejectedClubName
    );




    void sendEventApprovedEmail(
            String to,
            String eventName,
            long approvedPoints
    );


    void sendEventRejectedEmail(
            String to,
            String eventName,
            String reason,
            String staffName
    );



}
