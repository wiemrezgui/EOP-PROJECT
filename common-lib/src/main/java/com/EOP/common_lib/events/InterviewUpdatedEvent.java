package com.EOP.common_lib.events;

import com.EOP.common_lib.common.enums.InterviewMode;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class InterviewUpdatedEvent {
    private String interviewerEmail;
    private String candidateEmail;
    private String jobTitle;
    private LocalDate previousDate;
    private LocalTime previousTime;
    private LocalDate newDate;
    private LocalTime newTime;
    private InterviewMode mode;
    private String location;
    private String meetingLink;
    private String description;
    private boolean timeChanged;
    private boolean modeChanged;
    private boolean locationChanged;
}