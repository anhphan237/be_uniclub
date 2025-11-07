package com.example.uniclub.scheduler;

import com.example.uniclub.entity.Event;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.EventStatusEnum;
import com.example.uniclub.repository.EventRepository;
import com.example.uniclub.repository.EventRegistrationRepository;
import com.example.uniclub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventReminderScheduler {

    private final EventRepository eventRepo;
    private final EventRegistrationRepository registrationRepo;
    private final EmailService emailService;

    // 🔁 Chạy mỗi 30 phút
    @Scheduled(cron = "0 */30 * * * *")
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();

        // Gửi nhắc 24h trước
        processReminders(now.plusHours(24), "24 hours");

        // Gửi nhắc 1h trước
        processReminders(now.plusHours(1), "1 hour");
    }

    private void processReminders(LocalDateTime targetTime, String label) {
        LocalDate targetDate = targetTime.toLocalDate();
        List<Event> events = eventRepo.findAllByStatusAndDate(EventStatusEnum.APPROVED, targetDate);

        for (Event event : events) {
            LocalDateTime eventStart = LocalDateTime.of(event.getDate(), event.getStartTime());
            long diff = java.time.Duration.between(LocalDateTime.now(), eventStart).toMinutes();

            if (Math.abs(diff - (label.equals("24 hours") ? 1440 : 60)) <= 15) {
                List<User> users = registrationRepo.findUsersByEventId(event.getEventId());
                for (User u : users) {
                    sendReminderEmail(u, event, label);
                }
            }
        }
    }

    private void sendReminderEmail(User user, Event event, String label) {
        String subject = " Reminder: " + event.getName() + " starts in " + label;
        String body = """
                <p>Hi <b>%s</b>,</p>
                <p>Your registered event <b>%s</b> will start in %s.</p>
                <p>Date: %s<br> Time: %s</p>
                <p>Don't forget to check in using your code: <b>%s</b></p>
                """.formatted(
                user.getFullName(),
                event.getName(),
                label,
                event.getDate(),
                event.getStartTime(),
                event.getCheckInCode()
        );
        emailService.sendEmail(user.getEmail(), subject, body);
    }
}
