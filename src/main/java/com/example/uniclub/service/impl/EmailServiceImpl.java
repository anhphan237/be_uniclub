package com.example.uniclub.service.impl;

import com.example.uniclub.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // ✅ true = multipart mode (for inline images)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            // ✅ Cẩn thận: setFrom có 2 tham số cần encoding chuẩn
            helper.setFrom("uniclub.contacts@gmail.com", "UniClub System");
            helper.setTo(to);
            helper.setSubject(subject);


            String html = String.format("""
<div style="font-family: Arial, sans-serif; background: #F7FBFF; 
            border-radius: 12px; padding: 28px; max-width: 600px; margin: auto;
            box-shadow: 0 0 10px rgba(0,0,0,0.08); color-scheme: light !important;">

    <div style="text-align: center; margin-bottom: 22px;">
        <img src='cid:uniclub-logo' alt='UniClub Logo' style='width: 120px;'>
    </div>

    <div style="font-size: 16px; color: #111111 !important; line-height: 1.6;">
        %s
    </div>

    <hr style="margin: 28px 0; border: none; border-top: 1px solid #e2e2e2;">

    <div style="text-align: center; font-size: 14px; color: #444 !important;">
        <p>Best regards,<br><b>UniClub Vietnam</b><br>Digitalizing Communities 💡</p>
    </div>

</div>
""", content);


            // ✅ Đặt nội dung HTML
            helper.setText(html, true);

            // ✅ Inline logo (đảm bảo file nằm đúng đường dẫn)
            helper.addInline("uniclub-logo", new ClassPathResource("static/images/Logo.png"));

            mailSender.send(message);
            System.out.println(" Email sent successfully to " + to);

        } catch (MessagingException e) {
            System.err.println(" Messaging error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println(" Email send failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}