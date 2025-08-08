package com.EOP.interview_service.DTOs;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
@Getter @Setter @AllArgsConstructor
public class CreateInterviewRequestDTO {
    @NotNull
    private LocalDate scheduledDate;

    @NotNull
    private LocalTime scheduledTime;

    private String description;

    @NotNull
    private Long candidateID;

    @NotNull
    private String userUUID;
}
