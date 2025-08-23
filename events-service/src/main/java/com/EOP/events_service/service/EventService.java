package com.EOP.events_service.service;

import com.EOP.common_lib.common.enums.InterviewMode;
import com.EOP.common_lib.events.*;
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
    @KafkaListener(topics = "interview-created")
    public void handleInterviewCreated(InterviewCreatedEvent event) throws MessagingException, IOException, jakarta.mail.MessagingException {
        log.info("Received interview-created event: {}", event);
        try {
            // Send to candidate
            sendInterviewEmail(
                    event.getCandidateEmail(),
                    "Your Interview Scheduled: " + event.getJobTitle(),
                    event,
                    false // isInterviewer
            );

            // Send to interviewer
            sendInterviewEmail(
                    event.getUserEmail(),
                    "Interview Scheduled with Candidate",
                    event,
                    true // isInterviewer
            );

        } catch (Exception e) {
            log.error("Error processing interview notification", e);
        }
    }
    @KafkaListener(topics = "interview-updated")
    public void handleInterviewUpdated(InterviewUpdatedEvent event) throws IOException, jakarta.mail.MessagingException {
        log.info("Processing interview update");
        log.info("previous date {}", event.getPreviousDate());
        log.info("previous time {}", event.getPreviousTime());
        log.info("new date {}", event.getNewDate());
        log.info("new time {}", event.getNewTime());
        // Send to candidate
        sendInterviewUpdateEmail(
                event.getCandidateEmail(),
                "Updated: Your Interview for " + event.getJobTitle(),
                event,
                false
        );

        // Send to interviewer
        sendInterviewUpdateEmail(
                event.getInterviewerEmail(),
                "Updated: Interview with Candidate",
                event,
                true
        );
    }

    @KafkaListener(topics = "interview-cancelled")
    public void handleInterviewCancelled(InterviewCancelledEvent event)
            throws MessagingException, IOException, jakarta.mail.MessagingException {
        log.info("Processing interview cancellation");

        // Send to candidate
        sendInterviewCancellationEmail(
                event.getCandidateEmail(),
                "Cancelled: Interview for " + event.getJobTitle(),
                event,
                false
        );

        // Send to interviewer
        sendInterviewCancellationEmail(
                event.getInterviewerEmail(),
                "Cancelled: Interview with Candidate",
                event,
                true
        );
    }
    private void sendInterviewEmail(
            String recipientEmail,
            String subject,
            InterviewCreatedEvent event,
            boolean isInterviewer
    ) throws MessagingException, IOException, jakarta.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(noreplyEmail);
        helper.setTo(recipientEmail);
        helper.setSubject(subject);

        String htmlContent = loadTemplate("templates/interview-scheduled.html");
        Map<String, String> variables = new HashMap<>();

        // Common variables
        variables.put("jobTitle", event.getJobTitle());
        variables.put("scheduledDate", event.getInterviewDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        variables.put("scheduledTime", event.getInterviewTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        variables.put("duration", String.valueOf(event.getDurationMinutes()));
        variables.put("mode", event.getMode().toString());
        variables.put("description", event.getDescription() != null ? event.getDescription() : "");

        variables.put("emailTitle", isInterviewer ? "Interview Scheduled" : "Your Interview Scheduled");
        variables.put("onlineInstructions", isInterviewer ?
                "The candidate will join using the link above." :
                "Please join using the link above at the scheduled time.");
        variables.put("buttonText", isInterviewer ? "Start Meeting" : "Join Meeting");
        variables.put("rescheduleInstructions", isInterviewer ?
                "If you need to reschedule, please contact the recruitment team." :
                "If you need to reschedule, please contact your recruiter.");

        // Role-specific variables
        variables.put("isInterviewer", String.valueOf(isInterviewer));
        variables.put("otherParty", isInterviewer ? event.getCandidateEmail() : event.getUserEmail());
        variables.put("otherPartyLabel", isInterviewer ? "Candidate:" : "Interviewer:");

        if (event.getMode() == InterviewMode.ONLINE) {
            variables.put("meetingLink", event.getMeetingLink());
            variables.put("location", "Online Meeting");
        } else {
            variables.put("location", event.getLocation());
            variables.put("meetingLink", "#"); // Empty link for safety
        }

        // processor that handles conditionals
        String finalContent = processInterviewScheduleTemplateWithConditionals(htmlContent, variables);
        helper.setText(finalContent, true);

        ClassPathResource logo = new ClassPathResource("images/EOP-logo.png");
        helper.addInline("logo", logo);

        mailSender.send(message);
        log.info("Email sent successfully to {} for interview: {}", recipientEmail, event.getJobTitle());
    }
    private void sendInterviewUpdateEmail(String recipient, String subject,
                                          InterviewUpdatedEvent event, boolean isInterviewer)
            throws MessagingException, IOException, jakarta.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(noreplyEmail);
        helper.setTo(recipient);
        helper.setSubject(subject);

        String htmlContent = loadTemplate("templates/interview-updated.html");
        Map<String, String> variables = createUpdateEmailVariables(event, isInterviewer);

        String finalContent = replaceVariables(htmlContent, variables);

        helper.setText(finalContent, true);
        ClassPathResource logo = new ClassPathResource("images/EOP-logo.png");
        helper.addInline("logo", logo);

        mailSender.send(message);
        log.info("Interview update email sent to {}", recipient);
    }

    private void sendInterviewCancellationEmail(String recipient, String subject,
                                                InterviewCancelledEvent event, boolean isInterviewer)
            throws MessagingException, IOException, jakarta.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(noreplyEmail);
        helper.setTo(recipient);
        helper.setSubject(subject);

        String htmlContent = loadTemplate("templates/interview-cancelled.html");
        Map<String, String> variables = createCancellationEmailVariables(event, isInterviewer);
        String finalContent = replaceVariables(htmlContent, variables);

        helper.setText(finalContent, true);
        ClassPathResource logo = new ClassPathResource("images/EOP-logo.png");
        helper.addInline("logo", logo);

        mailSender.send(message);
        log.info("Interview cancellation email sent to {}", recipient);
    }

    private Map<String, String> createUpdateEmailVariables(InterviewUpdatedEvent event, boolean isInterviewer) {
        Map<String, String> vars = new HashMap<>();
        // Basic variables
        vars.put("jobTitle", event.getJobTitle() != null ? event.getJobTitle() : "");
        vars.put("otherParty", isInterviewer ?
                (event.getCandidateEmail() != null ? event.getCandidateEmail() : "") :
                (event.getInterviewerEmail() != null ? event.getInterviewerEmail() : ""));
        vars.put("otherPartyLabel", isInterviewer ? "Candidate:" : "Interviewer:");
        vars.put("newDate", event.getNewDate() != null ?
                event.getNewDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")) : "");
        vars.put("newTime", event.getNewTime() != null ?
                event.getNewTime().format(DateTimeFormatter.ofPattern("h:mm a")) : "");
        vars.put("mode", event.getMode() != null ? event.getMode().toString() : "");
        vars.put("location", event.getLocation() != null ? event.getLocation() : "");
        vars.put("meetingLink", event.getMeetingLink() != null ? event.getMeetingLink() : "");
        vars.put("description", event.getDescription() != null ? event.getDescription() : "");
        vars.put("contactPerson", isInterviewer ? "the recruitment team" : "your recruiter");

        // Conditional sections - built in Java
        boolean timeChanged = event.isTimeChanged();
        boolean modeChanged = event.isModeChanged();
        String mode = event.getMode() != null ? event.getMode().toString() : "";

        // Previous time section (only show if time changed)
        if (timeChanged && event.getPreviousDate() != null && event.getPreviousTime() != null) {
            String previousDate = event.getPreviousDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            String previousTime = event.getPreviousTime().format(DateTimeFormatter.ofPattern("h:mm a"));
            vars.put("previousTimeSection",
                    "<div class='detail-row'>" +
                            "<span class='detail-label'>Previous Time:</span>" +
                            "<span>" + previousDate + " at " + previousTime + "</span>" +
                            "</div>");
        } else {
            vars.put("previousTimeSection", "");
        }

        // Time label (changes based on whether time was updated)
        vars.put("timeLabel", timeChanged ? "New Time:" : "Time:");

        // Mode change section (only show if mode changed)
        if (modeChanged) {
            vars.put("modeChangeSection",
                    "<div class='detail-row'>" +
                            "<span class='detail-label'>New Mode:</span>" +
                            "<span>" + mode + "</span>" +
                            "</div>");
        } else {
            vars.put("modeChangeSection", "");
        }

        // Meeting link section (only for online mode)
        if ("ONLINE".equals(mode) && event.getMeetingLink() != null) {
            vars.put("meetingLinkSection",
                    "<div class='detail-row'>" +
                            "<span class='detail-label'>Meeting Link:</span>" +
                            "<span class='meeting-link'>" +
                            "<a href='" + event.getMeetingLink() + "'>" + event.getMeetingLink() + "</a>" +
                            "</span>" +
                            "</div>");
        } else {
            vars.put("meetingLinkSection", "");
        }

        // Location section (only for in-person mode)
        if ("IN_PERSON".equals(mode) && event.getLocation() != null) {
            vars.put("locationSection",
                    "<div class='detail-row'>" +
                            "<span class='detail-label'>Location:</span>" +
                            "<span>" + event.getLocation() + "</span>" +
                            "</div>");
        } else {
            vars.put("locationSection", "");
        }

        // Description section (only if description exists)
        if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
            vars.put("descriptionSection",
                    "<div class='detail-row'>" +
                            "<span class='detail-label'>Details:</span>" +
                            "<span>" + event.getDescription() + "</span>" +
                            "</div>");
        } else {
            vars.put("descriptionSection", "");
        }

        return vars;
    }

    private Map<String, String> createCancellationEmailVariables(InterviewCancelledEvent event, boolean isInterviewer) {
        Map<String, String> vars = new HashMap<>();
        // Common variables
        vars.put("jobTitle", event.getJobTitle());
        vars.put("isInterviewer", String.valueOf(isInterviewer));
        vars.put("otherParty", isInterviewer ? event.getCandidateEmail() : event.getInterviewerEmail());
        vars.put("otherPartyLabel", isInterviewer ? "Candidate:" : "Interviewer:");

        // Cancellation-specific variables
        vars.put("scheduledDate", event.getScheduledDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        vars.put("scheduledTime", event.getScheduledTime().format(DateTimeFormatter.ofPattern("h:mm a")));
        vars.put("cancellationReason", event.getCancellationReason() != null ?
                event.getCancellationReason() : "No reason provided");
        String nextSteps = isInterviewer ?
                "The candidate has been notified. Please contact the recruitment team if you need to reschedule." :
                "We apologize for any inconvenience. Our team will contact you shortly to discuss next steps.";

        vars.put("nextSteps", nextSteps);
        return vars;
    }
    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String value = entry.getValue();
            // Handle null values by replacing with empty string
            String replacement = value != null ? value : "";
            result = result.replace("${" + entry.getKey() + "}", replacement);
        }
        return result;
    }
    private String processInterviewScheduleTemplateWithConditionals(String template, Map<String, String> variables) {
        String result = template;

        // First, replace all simple variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        // Process conditionals
        result = result.replace("${mode == 'ONLINE' ? '", "");
        result = result.replace("' : ''}", "");
        result = result.replace("${description ? '", "");
        result = result.replace("${isInterviewer == 'true' ? '", "");

        return result;
    }

}

