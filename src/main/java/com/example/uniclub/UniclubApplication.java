package com.example.uniclub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.uniclub")
@EnableScheduling // 🕒 Bật Scheduler cho ClubActivityScheduler & MemberLevelScheduler
public class UniclubApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniclubApplication.class, args);
        System.out.println("🚀 UniClub Backend is running with Scheduler enabled!");
    }
}
