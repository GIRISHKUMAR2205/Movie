package com.movie.mail_service.service;

import java.nio.charset.StandardCharsets;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.movie.mail_service.config.MailProperties;
import com.movie.mail_service.event.EmailVerificationMailEvent;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Override
    public void send(EmailVerificationMailEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(event.recipient());
            helper.setSubject(event.subject());
            helper.setText(event.body(), false);
            // Stable message identity helps downstream SMTP providers collapse a retry after a crash.
            message.setHeader("Message-ID", "<" + event.eventId() + "@showhub.mail>");
            mailSender.send(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to send mail event " + event.eventId(), exception);
        }
    }
}
