package com.EOP.jobs_service.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Job application request data")
public class CandidateApplicationRequest {
    @NotBlank
    @Email
    @Schema(description = "Candidate email", example = "john.doe@example.com")
    private String email;

    @NotNull
    @Schema(description = "Job ID to apply for", example = "1")
    private Long jobId;
}
