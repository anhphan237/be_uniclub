package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Club;
import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.repository.MembershipRepository;
import com.example.uniclub.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventReminderScheduler {

    private final EventRepository eventRepo;
    private final MembershipRepository membershipRepo;
    private final EventRegistrationRepository registrationRepo;
    private final EmailService emailService;

    // Chạy mỗi 30 phút
    @Scheduled(cron = "0 */30 * * * *")
    public void sendRegistrationDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();

        processDeadline(now, 24 * 60, "24 hours");
        processDeadline(now, 60, "1 hour");
    }

    private void processDeadline(LocalDateTime now, long targetMinutes, String label) {

        // 👍 Dùng method JPA có sẵn — đảm bảo không lỗi
        List<Event> events = eventRepo.findAllByStatus(EventStatusEnum.APPROVED);

        for (Event event : events) {

            if (event.getRegistrationDeadline() == null) continue;

            // Convert LocalDate -> 23:59:59
            LocalDateTime deadline = event.getRegistrationDeadline().atTime(23, 59, 59);

            long diff = Duration.between(now, deadline).toMinutes();

            if (Math.abs(diff - targetMinutes) <= 15) {
                sendReminderToClubMembers(event, deadline, label);
            }
        }
    }

    private void sendReminderToClubMembers(Event event, LocalDateTime deadline, String label) {

        List<Long> clubIds = new ArrayList<>();
        clubIds.add(event.getHostClub().getClubId());

        List<Club> coHosts = event.getCoHostedClubs();
        if (coHosts != null) {
            clubIds.addAll(coHosts.stream().map(Club::getClubId).toList());
        }

        List<User> clubMembers = membershipRepo.findActiveUsersByClubIds(clubIds);

        List<Long> registeredIds = registrationRepo.findUserIdsByEventId(event.getEventId());

        List<User> usersToRemind = clubMembers.stream()
                .filter(u -> !registeredIds.contains(u.getUserId()))
                .toList();

        for (User user : usersToRemind) {
            sendEmail(user, event, deadline, label);
        }
    }

    private void sendEmail(User user, Event event, LocalDateTime deadline, String label) {

        String subject = "[UniClub] Registration for " + event.getName() + " closes in " + label;

        String body = """
                <p>Hi <b>%s</b>,</p>
                <p>The registration deadline for the event <b>%s</b> is in <b>%s</b>.</p>
                <p><b>Deadline:</b> %s</p>
                <p>If you'd like to participate, please register soon.</p>
                <br>
                <p>Best regards,<br>UniClub Team</p>
                """
                .formatted(
                        user.getFullName(),
                        event.getName(),
                        label,
                        deadline
                );

        emailService.sendEmail(user.getEmail(), subject, body);
    }
}
