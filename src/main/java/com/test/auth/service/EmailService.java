package com.test.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String firstName, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Vérification de votre compte");
            helper.setText("""
                <div style="font-family:Arial;max-width:600px;margin:0 auto">
                    <h2>Bienvenue %s ! 👋</h2>
                    <p>Votre code de vérification :</p>
                    <div style="background:#f4f4f4;padding:20px;text-align:center;border-radius:8px">
                        <h1 style="color:#007bff;letter-spacing:8px">%s</h1>
                    </div>
                    <p>Ce code expire dans <strong>15 minutes</strong>.</p>
                </div>
                """.formatted(firstName, code), true);

            mailSender.send(message);
            System.out.println("✅ Email envoyé à : " + toEmail);

        } catch (Exception e) {
            throw new RuntimeException("Échec envoi email : " + e.getMessage());
        }
    }
}
