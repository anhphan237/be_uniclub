package com.example.uniclub.service.impl;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.Membership;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.ClubRoleEnum;
import com.example.uniclub.enums.EventCoHostStatusEnum;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.EmailService;
import com.example.uniclub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;
    private final MembershipRepository membershipRepo;

    @Override
    public void notifyEventCompleted(Event event) {
        String subject = "Event Completed: " + event.getName();
        String content = String.format("""
        <p>Dear <b>%s</b>,</p>
        <p>Your event <b>%s</b> has been successfully marked as completed.</p>
        <p>The system will now handle automatic point settlements and surplus point returns.</p>
        <p>Thank you for organizing activities with UniClub!</p>
    """, event.getHostClub().getName(), event.getName());

        // ✅ 1️⃣ Gửi thông báo cho CLB chủ trì (Host)
        List<Membership> hostLeaders = membershipRepo.findByClub_ClubIdAndClubRoleIn(
                event.getHostClub().getClubId(),
                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
        );

        sendEmailsToLeaders(hostLeaders, event.getHostClub().getName(), subject, content);

        // ✅ 2️⃣ Nếu có co-host, gửi cho từng CLB đồng tổ chức
        if (event.getCoHostRelations() != null && !event.getCoHostRelations().isEmpty()) {
            event.getCoHostRelations().stream()
                    .filter(rel -> rel.getStatus() == EventCoHostStatusEnum.APPROVED) // chỉ gửi cho cohost đã duyệt
                    .forEach(rel -> {
                        Club coClub = rel.getClub();
                        List<Membership> coLeaders = membershipRepo.findByClub_ClubIdAndClubRoleIn(
                                coClub.getClubId(),
                                List.of(ClubRoleEnum.LEADER, ClubRoleEnum.VICE_LEADER)
                        );

                        String coContent = String.format("""
                        <p>Dear <b>%s</b>,</p>
                        <p>Your club participated as a co-host in the event <b>%s</b>, which has now been marked as completed.</p>
                        <p>The UniClub system will automatically handle all settlements and point refunds.</p>
                        <p>Thank you for your collaboration!</p>
                    """, coClub.getName(), event.getName());

                        sendEmailsToLeaders(coLeaders, coClub.getName(), subject, coContent);
                    });
        }

        log.info("Completion notifications sent for event '{}' to all host & co-host leaders", event.getName());
    }

    /**
     * 🧩 Helper method: gửi email đến tất cả LEADER và VICE_LEADER trong 1 CLB
     */
    private void sendEmailsToLeaders(List<Membership> leaders, String clubName, String subject, String content) {
        if (leaders == null || leaders.isEmpty()) {
            log.warn("No leaders found for club '{}' when sending completion notice", clubName);
            return;
        }

        for (Membership m : leaders) {
            if (m.getUser() != null && m.getUser().getEmail() != null) {
                emailService.sendEmail(m.getUser().getEmail(), subject, content);
                log.info("Notification sent to {} ({}) for club '{}'",
                        m.getUser().getEmail(), m.getClubRole(), clubName);
            }
        }
    }




}
