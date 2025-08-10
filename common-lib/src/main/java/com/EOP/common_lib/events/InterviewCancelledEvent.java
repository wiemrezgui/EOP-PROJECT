package com.EOP.common_lib.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCancelledEvent {
    private String interviewerEmail;
    private String candidateEmail;
    private String jobTitle;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String cancellationReason;
}
