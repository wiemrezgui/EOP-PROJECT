package com.EOP.jobs_service.services;

import com.EOP.jobs_service.repositories.JobApplicationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobApplicationService {
 private final JobApplicationRepository jobApplicationRepository;

    public boolean verifyJobApplication(Long jobId,Long candidateId) {
        return jobApplicationRepository.existsByCandidateIdAndJobId(candidateId,jobId);
    }
}
