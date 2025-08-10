package com.EOP.interview_service.DTOs;

import com.EOP.interview_service.enums.InterviewMode;
import com.EOP.interview_service.enums.InterviewStatus;
import com.EOP.interview_service.enums.TimeRange;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter @Setter
public class InterviewFilterDTO {
    private InterviewMode mode; // ONLINE, IN_PERSON
    private InterviewStatus status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private TimeRange timeRange; // NEXT_3_DAYS, NEXT_WEEK, NEXT_MONTH, CUSTOM
}
