package com.EOP.common_lib.events;

import com.EOP.common_lib.common.enums.InterviewMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @AllArgsConstructor
public class InterviewCreatedEvent {
    private String userEmail;
    private String jobTitle;
    private String interviewTitle;
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private InterviewMode mode;
    private String location;
    private String meetingLink;
    private String description;
    private String candidateEmail;
    private Integer durationMinutes;

}
