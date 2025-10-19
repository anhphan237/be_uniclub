package com.example.uniclub.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class AppJwtProps {
    private String eventSecret; // đặt trong application.yml
}
