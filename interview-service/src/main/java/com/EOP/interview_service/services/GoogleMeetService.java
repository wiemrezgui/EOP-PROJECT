package com.EOP.interview_service.services;

import com.EOP.interview_service.DTOs.MeetingDetailsDTO;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.ConferenceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMeetService {

    private final Calendar calendar;

    public MeetingDetailsDTO createMeeting(String title, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Event event = createCalendarEvent(title, startTime, endTime);
            Event createdEvent = calendar.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute();

            String meetLink = extractMeetLink(createdEvent);

            log.info("Successfully created Google Meet: {}", meetLink);

            return new MeetingDetailsDTO(meetLink, title, startTime, endTime);

        } catch (IOException e) {
            log.error("Error creating Google Meet", e);
            throw new RuntimeException("Failed to create Google Meet", e);
        }
    }

    private Event createCalendarEvent(String title, LocalDateTime startTime, LocalDateTime endTime) {
        Event event = new Event()
                .setSummary(title)
                .setDescription("Interview meeting");

        // Convert LocalDateTime to Google DateTime
        DateTime startDateTime = new DateTime(startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);

        DateTime endDateTime = new DateTime(endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);

        // Create Google Meet conference
        ConferenceSolutionKey conferenceSolutionKey = new ConferenceSolutionKey();
        conferenceSolutionKey.setType("hangoutsMeet");

        CreateConferenceRequest createConferenceRequest = new CreateConferenceRequest();
        createConferenceRequest.setRequestId(UUID.randomUUID().toString());
        createConferenceRequest.setConferenceSolutionKey(conferenceSolutionKey);

        ConferenceData conferenceData = new ConferenceData();
        conferenceData.setCreateRequest(createConferenceRequest);

        event.setConferenceData(conferenceData);

        return event;
    }

    private String extractMeetLink(Event event) {
        if (event.getConferenceData() != null &&
                event.getConferenceData().getEntryPoints() != null &&
                !event.getConferenceData().getEntryPoints().isEmpty()) {

            return event.getConferenceData().getEntryPoints().get(0).getUri();
        }

        throw new RuntimeException("Failed to generate Google Meet link");
    }
}