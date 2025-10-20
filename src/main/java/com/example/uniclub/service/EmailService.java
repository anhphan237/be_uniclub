package com.example.uniclub.service;

public interface EmailService {
//    void sendEmail(String to, String subject, String content);
    void sendEmail(String to, String subject, String htmlBody);
}
