package com.example.uniclub.config;

import com.example.uniclub.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class CryptoConfig {
    @Bean
    public CryptoUtil cryptoUtil(@Value("${app.crypto.aes-gcm.key-b64}") String keyB64) {
        byte[] key = Base64.getDecoder().decode(keyB64); // 16/24/32 bytes => AES-128/192/256
        return new CryptoUtil(key);
    }
}