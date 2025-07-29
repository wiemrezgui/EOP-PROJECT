package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.models.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByCandidateEmailAndJobId(String email, Long jobId);
    Page<JobApplication> findByJobId(Long jobId, Pageable pageable);
}
