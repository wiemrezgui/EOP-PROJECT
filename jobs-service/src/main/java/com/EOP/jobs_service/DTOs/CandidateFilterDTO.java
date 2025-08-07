package com.EOP.jobs_service.DTOs;

import com.EOP.jobs_service.enums.CandidateStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CandidateFilterDTO {
    private List<CandidateStatus> statuses;
    private LocalDate appliedFrom;
    private LocalDate appliedTo;
    private Long jobId;
    private String educationLevel;
    private List<String> skills;
}
