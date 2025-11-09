package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;
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
                        "Best regards,\nUniClub Team ",
                clubName
        );
        emailService.sendEmail(studentEmail, subject, content);
    }

    // 📨 Khi club leader duyệt hoặc từ chối đơn
    @Override
    public void sendApplicationResult(String studentEmail, String clubName, boolean accepted) {
        String subject = accepted ?
                "Your application to " + clubName + " has been approved " :
                "Your application to " + clubName + " has been declined ";

        String content = accepted ?
                String.format("Hi,\n\nCongratulations! \nYour application to join %s has been approved.\nYou are now an official member of the club!\n\nWelcome aboard!\n\n— UniClub Team 💌", clubName)
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
                "Your event \"" + eventName + "\" has been approved " :
                "Your event \"" + eventName + "\" has been rejected ";

        String content = approved ?
                String.format("Hi Leader,\n\nYour event \"%s\" has been successfully approved by University Staff.\nYou can now start promoting it to your members!\n\n— UniClub Team 🎪", eventName)
                :
                String.format("Hi Leader,\n\nYour event \"%s\" has been rejected.\nPlease check the admin feedback in your UniClub dashboard.\n\n— UniClub Team 💌", eventName);

        emailService.sendEmail(leaderEmail, subject, content);
    }
    @Override
    public void notifyCoHostInvite(Club coHost, Event event) {
        System.out.printf("Notify Co-Host [%s] about new event [%s]%n",
                coHost.getName(), event.getName());
    }

    @Override
    public void notifyUniStaffWaiting(Event event) {
        System.out.printf("Notify UniStaff: Event [%s] waiting for co-host confirmation%n",
                event.getName());
    }

    @Override
    public void notifyHostEventRejectedByCoHost(Event event, Club coClub) {
        System.out.printf(" Notify Host [%s]: Co-Host [%s] rejected event [%s]%n",
                event.getHostClub().getName(), coClub.getName(), event.getName());
    }

    @Override
    public void notifyUniStaffReadyForReview(Event event) {
        System.out.printf(" Notify UniStaff: All co-hosts accepted event [%s], ready for review%n",
                event.getName());
    }

    @Override
    public void notifyEventRejected(Event event, User creator) {
        String subject = "Event Rejected: " + event.getName();
        String content = String.format(
                "Hi %s,\n\nYour event \"%s\" has been rejected by University Staff.\n" +
                        "Please review the feedback on your UniClub dashboard.\n\n" +
                        "— UniClub Team",
                creator.getFullName(), event.getName()
        );
        emailService.sendEmail(creator.getEmail(), subject, content);

        System.out.printf("Notify [%s]: Event [%s] rejected by UniStaff%n",
                creator.getFullName(), event.getName());
    }


    @Override
    public void notifyEventApproved(Event event) {
        System.out.printf(" Notify Host [%s]: Event [%s] approved by UniStaff%n",
                event.getHostClub().getName(), event.getName());
    }

}
