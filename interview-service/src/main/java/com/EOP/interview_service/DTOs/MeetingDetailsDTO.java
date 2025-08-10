package com.EOP.interview_service.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MeetingDetailsDTO {
    private String meetingLink;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
