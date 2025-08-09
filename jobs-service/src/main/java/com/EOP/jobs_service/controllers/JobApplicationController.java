package com.EOP.jobs_service.controllers;

import com.EOP.jobs_service.services.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-application")
@RequiredArgsConstructor
@Tag(name="Job application")
public class JobApplicationController {
   private final JobApplicationService jobApplicationService;
    @GetMapping("/validate-application/job/{jobId}/candidate/{candidateId}")
    @Operation(summary = "verify job application")
    public boolean validateJobApplication(
            @PathVariable Long jobId,
            @PathVariable Long candidateId) {

        return jobApplicationService.verifyJobApplication(jobId,candidateId);
    }
}
