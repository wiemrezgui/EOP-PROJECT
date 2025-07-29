package com.EOP.jobs_service.DTO;

import com.EOP.jobs_service.model.Candidate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "Candidate response data")
public class CandidateResponse {
    private Long id;
    private String email;
    private LocalDate appliedDate;
    private String status;
    private boolean hasResume;

    public CandidateResponse(Candidate candidate) {
        this.id = candidate.getId();
        this.email = candidate.getEmail();
        this.appliedDate = candidate.getAppliedDate();
        this.status = candidate.getStatus().name();
        this.hasResume = candidate.getResume() != null;
    }
}