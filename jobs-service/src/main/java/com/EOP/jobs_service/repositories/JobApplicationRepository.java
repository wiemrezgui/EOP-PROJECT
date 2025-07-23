package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
