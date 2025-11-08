package com.example.uniclub.message;

import com.example.uniclub.config.RabbitMQConfig;
import com.example.uniclub.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {
    private final RabbitTemplate rabbitTemplate;

    public void send(NotificationMessage msg) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                msg
        );
    }
}
