package com.example.uniclub.controller;

import com.example.uniclub.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailService emailService;

    @GetMapping
    public String sendTestEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content
    ) {
        emailService.sendEmail(to, subject, content);
        return "✅ Email sent to: " + to;
    }
}
