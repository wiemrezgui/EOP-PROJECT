package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
}
