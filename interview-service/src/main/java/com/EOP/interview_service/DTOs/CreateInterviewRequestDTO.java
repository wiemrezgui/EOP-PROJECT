package com.EOP.interview_service.DTOs;

import com.EOP.interview_service.enums.InterviewMode;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
@Getter @Setter @AllArgsConstructor
@NoArgsConstructor
public class CreateInterviewRequestDTO {
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduledDate;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime scheduledTime;

    private String description;
    private String meetingTitle;
    private String location;
    @Enumerated(EnumType.STRING)
    private InterviewMode mode;
    @NotNull
    private Long candidateID;
    @Min(15)
    @Max(90)
    private Integer durationMinutes = 15;
    @NotNull @Email
    private String userEmail;
    private Long JobID;

}
