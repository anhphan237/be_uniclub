package com.example.uniclub.service.impl;

import com.example.uniclub.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("uniclub.contacts@gmail.com", "UniClub Vietnam");
            helper.setTo(to);
            helper.setSubject(subject);

            // ✅ HTML template with logo and gradient background
            String html = """
                    <div style="font-family: Arial, sans-serif; background: linear-gradient(180deg, #EAF9FF 0%, #FFFFFF 100%);
                                border-radius: 10px; padding: 30px; max-width: 600px; margin: auto; box-shadow: 0 0 12px rgba(0,0,0,0.1);">
                        <div style="text-align: center; margin-bottom: 25px;">
                            <img src='cid:uniclub-logo' alt='UniClub Logo' style='width: 110px;'>
                        </div>
                        <div style="font-size: 16px; color: #333;">
                            %s
                        </div>
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                        <div style="text-align: center; font-size: 14px; color: #777;">
                            <p>Best regards,<br><b>UniClub Vietnam</b><br>Digitalizing Communities 💡</p>
                        </div>
                    </div>
                    """.formatted(content);

            helper.setText(html, true);
            helper.addInline("uniclub-logo", new ClassPathResource("static/images/Logo.png"));

            mailSender.send(message);
            System.out.println("✅ Email sent successfully to " + to);

        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("❌ Email send failed: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
