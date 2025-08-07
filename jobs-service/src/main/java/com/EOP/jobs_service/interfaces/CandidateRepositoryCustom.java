package com.EOP.jobs_service.interfaces;

import com.EOP.jobs_service.DTOs.CandidateFilterDTO;
import com.EOP.jobs_service.models.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CandidateRepositoryCustom {
    Page<Candidate> findWithFilters(CandidateFilterDTO filters, Pageable pageable);
    long countWithFilters(CandidateFilterDTO filters);
}