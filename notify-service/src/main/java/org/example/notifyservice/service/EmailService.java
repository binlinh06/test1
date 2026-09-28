package org.example.notifyservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${notification.mail.from:noreply@example.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendOrderCreatedEmail(String recipient) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject("Appointment Confirmation");
            message.setText("Your appointment request has been successfully created for email: " + recipient);

            mailSender.send(message);
            log.info("Successfully sent email to {}", recipient);
        } catch (Exception e) {
            log.error("Could not send email to {}. Error: {}", recipient, e.getMessage());
        }
    }
}