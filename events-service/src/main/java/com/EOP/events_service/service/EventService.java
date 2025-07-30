package com.EOP.events_service.service;

import events.AccountCreatedEvent;
import events.JobApplicationEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class EventService {
    private final JavaMailSender mailSender;
    private final static String emailVerificationPath = "templates/email-verification.html";

    @Value("${spring.mail.properties.from}")
    private String noreplyEmail;

    @KafkaListener(topics = "account-created")
    public void handleAccountCreated(AccountCreatedEvent event) throws MessagingException, IOException, jakarta.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        // Base configuration
        helper.setFrom(noreplyEmail);
        helper.setTo(event.getEmail());
        helper.setSubject("Your Account Credentials");

        // Load and process template
        String htmlContent = loadTemplate(emailVerificationPath);
        Map<String, String> variables = new HashMap<>();
        variables.put("username", event.getUsername());
        variables.put("email", event.getEmail());
        variables.put("password", event.getPassword());
        variables.put("verificationLink",
                "https://localhost:8080/verify-account?email=" + event.getEmail());

        String finalContent = replaceVariables(htmlContent, variables);
        helper.setText(finalContent, true);

        ClassPathResource logo = new ClassPathResource("images/EOP-logo.png");
        helper.addInline("logo", logo);

        mailSender.send(message);
    }
    @KafkaListener(topics = "job-application")
    public void handleJobApplication(JobApplicationEvent event) throws MessagingException, IOException, jakarta.mail.MessagingException {
        log.info("Received job application event: {}", event);
        log.info("Processing job application for: {} - Job: {}", event.getCandidateEmail(), event.getJobTitle());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(noreplyEmail);
            helper.setTo(event.getCandidateEmail());
            helper.setSubject("Application Confirmation: " + event.getJobTitle());

            String htmlContent = loadTemplate("templates/job-application.html");
            Map<String, String> variables = new HashMap<>();
            variables.put("jobTitle", event.getJobTitle());
            variables.put("applicationDate", event.getApplicationDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            String finalContent = replaceVariables(htmlContent, variables);
            helper.setText(finalContent, true);

            ClassPathResource logo = new ClassPathResource("images/EOP-logo.png");
            helper.addInline("logo", logo);

            mailSender.send(message);
            log.info("Email sent successfully for job application: {}", event.getJobTitle());
        } catch (Exception e) {
            log.error("Error processing job application event", e);
            throw e;
        }
    }
    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

