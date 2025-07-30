package com.EOP.jobs_service.DTOs;

import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.models.CandidateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "Candidate response data")
@AllArgsConstructor
public class CandidateResponse {
    private Long id;
    private String email;
    private LocalDate appliedDate;
    private String status;

    public CandidateResponse(Candidate candidate) {
        this.id = candidate.getId();
        this.email = candidate.getEmail();
        this.appliedDate = candidate.getAppliedDate();
        this.status = candidate.getStatus().name();
    }
}