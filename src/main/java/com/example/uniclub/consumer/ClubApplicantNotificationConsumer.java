package com.example.uniclub.consumer;

import com.example.uniclub.config.RabbitMQConfig;
import com.example.uniclub.message.ClubApplicantMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClubApplicantNotificationConsumer {

    @RabbitListener(queues = RabbitMQConfig.CLUB_APP_QUEUE)
    public void handleClubApplicantStatus(ClubApplicantMessage message) {
        log.info("📩 Nhận được message: {}", message);
    }
}
