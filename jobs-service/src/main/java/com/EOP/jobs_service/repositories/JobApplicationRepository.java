package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.models.JobApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    boolean existsByCandidateEmailAndJobId(String email, Long jobId);
    @Query("SELECT ja.candidate " +  // Fetch the entire candidate entity
            "FROM JobApplication ja " +
            "WHERE ja.job.id = :jobId")
    Page<Candidate> findCandidatesByJobId(@Param("jobId") Long jobId, Pageable pageable);
    @Cacheable(value = "job_applications_count", key = "#jobId")
    int countByJobId(Long jobId);
}
