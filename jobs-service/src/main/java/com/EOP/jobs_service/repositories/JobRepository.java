package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.models.Job;
import com.EOP.jobs_service.models.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {
    @Query("SELECT j FROM Job j ORDER BY j.postedDate DESC")
    Page<Job> findAllJobs(Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.status = :status ORDER BY j.postedDate DESC")
    Page<Job> findByStatus(@Param("status") JobStatus status, Pageable pageable);
}
