package com.example.uniclub.service.impl;

import com.example.uniclub.dto.response.SystemStatusResponse;
import com.example.uniclub.service.AdminMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminMonitorServiceImpl implements AdminMonitorService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${spring.profiles.active:dev}")
    private String environment;

    @Override
    public SystemStatusResponse getSystemStatus() {
        boolean dbOk = true;
        boolean redisOk = true;
        boolean mqOk = true;

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            dbOk = false;
        }

        try {
            redisTemplate.opsForValue().set("health_check", "OK");
        } catch (Exception e) {
            redisOk = false;
        }

        try {
            rabbitTemplate.convertAndSend("health.test", "ping");
        } catch (Exception e) {
            mqOk = false;
        }

        return SystemStatusResponse.builder()
                .databaseUp(dbOk)
                .redisUp(redisOk)
                .rabbitmqUp(mqOk)
                .cloudinaryUp(true) // ⚠️ sau này có thể check qua API
                .environment(environment)
                .appVersion("1.0.0-ADMIN")
                .lastCheckedAt(LocalDateTime.now().toString())
                .build();
    }
}
