package com.EOP.jobs_service.DTO;

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

    @Schema(description = "Resume file (PDF preferred)")
    private MultipartFile resume;
}

