package com.EOP.interview_service.repositories;

import com.EOP.interview_service.enums.InterviewMode;
import com.EOP.interview_service.enums.InterviewStatus;
import com.EOP.interview_service.models.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Page<Interview> findByCandidateID(Long candidateId, Pageable pageable);
    Page<Interview> findByStatus(InterviewStatus status, Pageable pageable);
    Page<Interview> findByMode(InterviewMode mode, Pageable pageable);
    long countByCandidateID(Long candidateId);
    long countByStatus(InterviewStatus status);
    long countByMode(InterviewMode mode);
}
