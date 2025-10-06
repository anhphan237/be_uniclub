package com.example.uniclub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.uniclub")
public class UniclubApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniclubApplication.class, args);
    }
}
