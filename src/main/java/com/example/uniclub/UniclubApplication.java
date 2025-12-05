package com.example.uniclub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = "com.example.uniclub")
@EnableScheduling
public class UniclubApplication {

    public static void main(String[] args) {
        // Đặt timezone TRƯỚC khi Spring Boot chạy
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SpringApplication.run(UniclubApplication.class, args);
        System.out.println("UniClub Backend is running with VN timezone!");
    }
}

