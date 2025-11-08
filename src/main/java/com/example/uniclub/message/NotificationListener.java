package com.example.uniclub.message;

import com.example.uniclub.config.RabbitMQConfig;
import com.example.uniclub.dto.NotificationMessage;
import com.example.uniclub.entity.Notification;
import com.example.uniclub.entity.User;
import com.example.uniclub.enums.NotificationStatusEnum;
import com.example.uniclub.repository.NotificationRepository;
import com.example.uniclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationListener {
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage msg) {
        try {
            User user = userRepo.findById(msg.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + msg.getUserId()));

            Notification n = Notification.builder()
                    .user(user)
                    .message(msg.getMessage())
                    .type(msg.getType())
                    .status(NotificationStatusEnum.UNREAD)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepo.save(n);

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(user.getUserId()),
                    "/queue/notifications",
                    n
            );

            log.info("📩 Sent realtime notification to user {}", user.getFullName());
        } catch (Exception e) {
            log.error("❌ Notification error: {}", e.getMessage());
        }
    }
}
