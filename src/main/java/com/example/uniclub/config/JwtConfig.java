package com.example.uniclub.config;

import com.example.uniclub.service.JwtEventTokenService;
import com.example.uniclub.service.impl.JwtEventTokenServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Bean
    public JwtEventTokenService jwtEventTokenService() {
        return new JwtEventTokenServiceImpl();
    }
}
