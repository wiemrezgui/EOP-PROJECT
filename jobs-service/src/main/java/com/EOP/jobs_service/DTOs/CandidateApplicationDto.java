package com.EOP.jobs_service.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Schema(description = "Candidate job application data")
public class CandidateApplicationDto {
    @Email
    @Schema(description = "Candidate email", example = "john.doe@example.com")
    private String email;

    @NotNull
    @Schema(description = "Job ID to apply for", example = "1")
    private Long jobId;

    @NotNull
    @Schema(description = "Job title to apply for", example = "Full stack developer")
    private String jobTitle;

    @Schema(description = "Resume file (PDF preferred)")
    private MultipartFile resume;
}

