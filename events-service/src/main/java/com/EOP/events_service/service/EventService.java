package com.EOP.events_service.service;

import events.AccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EventService {
    private final JavaMailSender mailSender;

    @KafkaListener(topics = "account-created")
    public void handleAccountCreated(AccountCreatedEvent event) {
        System.out.println("Received account created event");
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(event.getEmail());
        email.setSubject("Verify Your Account");
        email.setText(String.format(
                "Hi %s, click to verify: http://localhost:8080/verify?token=%s",
                event.getUsername(),
                event.getPassword()
        ));
        this.mailSender.send(email);
    }
}

