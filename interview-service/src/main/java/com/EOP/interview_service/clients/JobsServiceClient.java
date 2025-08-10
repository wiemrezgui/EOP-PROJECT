package com.EOP.interview_service.clients;

import com.EOP.common_lib.common.DTO.ApiResponse;
import com.EOP.interview_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "jobs-service",
        url = "${jobs.service.url}",
        configuration = FeignClientConfig.class
)
public interface JobsServiceClient {
    @GetMapping("/api/jobs/candidates/check-candidate/{candidateID}")
    Boolean checkCandidateExists(
            @PathVariable Long candidateID
    );
    @GetMapping("/api/job-application/validate-application/job/{jobId}/candidate/{candidateId}")
    Boolean validateJobApplication(
            @PathVariable Long jobId,
            @PathVariable Long candidateId
    );
    @GetMapping("/api/jobs/candidates/get-email-by-id/{id}")
    String getCandidateEmailById(@PathVariable Long id);
    @GetMapping("/api/jobs/title/{id}")
    String getJobTitleById(@PathVariable Long id);
}
