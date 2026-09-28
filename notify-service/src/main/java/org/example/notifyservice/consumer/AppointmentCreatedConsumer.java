package org.example.notifyservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notifyservice.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCreatedConsumer {
    private final EmailService emailService;

    @KafkaListener(topics = "${notification.kafka.order-created-topic:appointment-created}", groupId = "${spring.kafka.consumer.group-id:notify-group}")
    public void consume(String email) {
        log.info("Received Kafka event for appointment creation, recipient: {}", email);
        emailService.sendOrderCreatedEmail(email);
    }
}

