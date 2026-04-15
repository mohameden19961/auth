package com.test.auth.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${spring.mail.username:}")
    private String gmailUsername;

    @Value("${spring.mail.password:}")
    private String gmailPassword;

    public void sendVerificationEmail(String toEmail, String firstName, String code) {
        if (resendApiKey != null && !resendApiKey.isEmpty()) {
            sendWithResend(toEmail, firstName, code);
        } else {
            sendWithGmail(toEmail, firstName, code);
        }
    }

    private void sendWithResend(String toEmail, String firstName, String code) {
        try {
            Resend resend = new Resend(resendApiKey);
            String html = """
                <div style="font-family:Arial;max-width:600px;margin:0 auto">
                    <h2>Bienvenue %s ! 👋</h2>
                    <p>Votre code de vérification :</p>
                    <div style="background:#f4f4f4;padding:20px;text-align:center;border-radius:8px">
                        <h1 style="color:#007bff;letter-spacing:8px">%s</h1>
                    </div>
                    <p>Ce code expire dans <strong>15 minutes</strong>.</p>
                </div>
                """.formatted(firstName, code);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("onboarding@resend.dev")
                    .to(toEmail)
                    .subject("🔐 Vérification de votre compte")
                    .html(html)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("✅ Email Resend envoyé : " + response.getId());
        } catch (ResendException e) {
            throw new RuntimeException("Échec envoi email : " + e.getMessage());
        }
    }

    private void sendWithGmail(String toEmail, String firstName, String code) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props,
                new jakarta.mail.Authenticator() {
                    protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new jakarta.mail.PasswordAuthentication(gmailUsername, gmailPassword);
                    }
                });

            jakarta.mail.internet.MimeMessage message = new jakarta.mail.internet.MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(gmailUsername));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO,
                jakarta.mail.internet.InternetAddress.parse(toEmail));
            message.setSubject("🔐 Vérification de votre compte");
            message.setContent("""
                <div style="font-family:Arial">
                    <h2>Bienvenue %s ! 👋</h2>
                    <h1 style="color:#007bff;letter-spacing:8px">%s</h1>
                    <p>Expire dans 15 minutes.</p>
                </div>
                """.formatted(firstName, code), "text/html; charset=utf-8");

            jakarta.mail.Transport.send(message);
            System.out.println("✅ Email Gmail envoyé à : " + toEmail);
        } catch (Exception e) {
            throw new RuntimeException("Échec envoi email Gmail : " + e.getMessage());
        }
    }
}
