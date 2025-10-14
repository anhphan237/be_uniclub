package com.example.uniclub.service.impl;

import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;

    // 📨 Khi student nộp đơn apply club
    @Override
    public void sendApplicationSubmitted(String studentEmail, String clubName) {
        String subject = "Your application to " + clubName + " has been received 🎉";
        String content = String.format(
                "Hi there,\n\n" +
                        "Thank you for applying to join the club \"%s\".\n" +
                        "Your application has been successfully submitted and is now waiting for the club leader's review.\n\n" +
                        "You will receive an update once your application is processed.\n\n" +
                        "Best regards,\nUniClub Team 💌",
                clubName
        );
        emailService.sendEmail(studentEmail, subject, content);
    }

    // 📨 Khi club leader duyệt hoặc từ chối đơn
    @Override
    public void sendApplicationResult(String studentEmail, String clubName, boolean accepted) {
        String subject = accepted ?
                "Your application to " + clubName + " has been approved ✅" :
                "Your application to " + clubName + " has been declined ❌";

        String content = accepted ?
                String.format("Hi,\n\nCongratulations! 🎉\nYour application to join %s has been approved.\nYou are now an official member of the club!\n\nWelcome aboard!\n\n— UniClub Team 💌", clubName)
                :
                String.format("Hi,\n\nUnfortunately, your application to join %s has been declined.\nYou can apply again later or explore other clubs on UniClub.\n\n— UniClub Team 💌", clubName);

        emailService.sendEmail(studentEmail, subject, content);
    }

    // 📨 Khi club gửi event mới cho staff duyệt
    @Override
    public void sendEventApprovalRequest(String staffEmail, String clubName, String eventName) {
        String subject = "Event Approval Request: " + eventName;
        String content = String.format(
                "Dear University Staff,\n\n" +
                        "The club \"%s\" has submitted a new event request titled \"%s\".\n" +
                        "Please review and approve or reject this event in the UniClub system.\n\n" +
                        "— UniClub Notification System 💼",
                clubName, eventName
        );
        emailService.sendEmail(staffEmail, subject, content);
    }

    // 📨 Khi staff duyệt hoặc từ chối event
    @Override
    public void sendEventApprovalResult(String leaderEmail, String eventName, boolean approved) {
        String subject = approved ?
                "Your event \"" + eventName + "\" has been approved ✅" :
                "Your event \"" + eventName + "\" has been rejected ❌";

        String content = approved ?
                String.format("Hi Leader,\n\nYour event \"%s\" has been successfully approved by University Staff.\nYou can now start promoting it to your members!\n\n— UniClub Team 🎪", eventName)
                :
                String.format("Hi Leader,\n\nYour event \"%s\" has been rejected.\nPlease check the admin feedback in your UniClub dashboard.\n\n— UniClub Team 💌", eventName);

        emailService.sendEmail(leaderEmail, subject, content);
    }
}
