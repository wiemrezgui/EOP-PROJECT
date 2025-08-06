package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.enums.CandidateStatus;
import jakarta.validation.constraints.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    @Query("SELECT c FROM Candidate c ORDER BY c.appliedDate DESC")
    Page<Candidate> findAllCandidates(Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE c.status = :status ORDER BY c.appliedDate DESC")
    Page<Candidate> findByStatus(@Param("status") CandidateStatus status, Pageable pageable);

    Optional<Candidate> findByEmail(@Email String email);

}
