package com.example.uniclub.controller;

import com.rabbitmq.client.Channel;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final ConnectionFactory connectionFactory;

    @Operation(summary = "Kiểm tra số message đang chờ trong RabbitMQ queue")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        try (var connection = connectionFactory.createConnection();
             Channel channel = connection.createChannel(false)) {

            var queueName = "uniclub-notify-queue";
            var result = channel.queueDeclarePassive(queueName);

            return ResponseEntity.ok(Map.of(
                    "queue", queueName,
                    "messageCount", result.getMessageCount(),
                    "consumerCount", result.getConsumerCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Cannot connect to RabbitMQ: " + e.getMessage()));
        }
    }
}
