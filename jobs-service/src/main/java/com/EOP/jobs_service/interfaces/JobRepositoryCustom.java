package com.EOP.jobs_service.interfaces;

import com.EOP.jobs_service.DTOs.JobFilterDTO;
import com.EOP.jobs_service.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobRepositoryCustom {
    Page<Job> findWithFilters(JobFilterDTO filters, Pageable pageable);
    long countWithFilters(JobFilterDTO filters);
}