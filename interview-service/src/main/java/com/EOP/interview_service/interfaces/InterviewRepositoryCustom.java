package com.EOP.interview_service.interfaces;

import com.EOP.interview_service.DTOs.InterviewFilterDTO;
import com.EOP.interview_service.models.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InterviewRepositoryCustom {
    Page<Interview> findWithFilters(InterviewFilterDTO filters, Pageable pageable);
    long countWithFilters(InterviewFilterDTO filters);
}
