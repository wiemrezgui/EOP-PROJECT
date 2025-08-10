package com.EOP.jobs_service.services;

import com.EOP.common_lib.common.exceptions.ResourceNotFoundException;
import com.EOP.common_lib.events.JobApplicationEvent;
import com.EOP.jobs_service.DTOs.CandidateApplicationDto;
import com.EOP.jobs_service.DTOs.CandidateFilterDTO;
import com.EOP.jobs_service.DTOs.CandidateResponse;
import com.EOP.jobs_service.exceptions.*;
import com.EOP.jobs_service.interfaces.CandidateRepositoryCustom;
import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.enums.CandidateStatus;
import com.EOP.jobs_service.models.Job;
import com.EOP.jobs_service.models.JobApplication;
import com.EOP.jobs_service.repositories.CandidateRepository;
import com.EOP.jobs_service.repositories.JobApplicationRepository;
import com.EOP.jobs_service.repositories.JobRepository;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateService {
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateRepositoryCustom candidateRepositoryCustom;
    private final KafkaTemplate<String, JobApplicationEvent> kafkaTemplate;
    // Cache keys
    private static final String ALL_CANDIDATES_CACHE = "all_candidates";
    private static final String CANDIDATES_COUNT_CACHE = "candidates-count";
    private static final String CANDIDATE_BY_ID_CACHE = "candidate_";
    private static final String CANDIDATE_BY_EMAIL_CACHE = "candidate_email_";
    private static final String JOB_APPLICATIONS_CACHE = "job_applications";
    private static final String CANDIDATES_FILTERED_CACHE = "filtered_candidates";
    private static final String CANDIDATES_FILTERED_COUNT_CACHE = "filtered_candidates_count";

    @CacheEvict(value = {ALL_CANDIDATES_CACHE, JOB_APPLICATIONS_CACHE}, allEntries = true)
    public Candidate applyForJob(CandidateApplicationDto applicationDto) throws IOException {
        // Validate duplicate application
        if (jobApplicationRepository.existsByCandidateEmailAndJobId(
                applicationDto.getEmail(),
                applicationDto.getJobId())) {
            throw new AppliedJobException("You've already applied for this job");
        }

        // Find or create candidate
        Candidate candidate = candidateRepository.findByEmail(applicationDto.getEmail())
                .orElseGet(() -> {
                    Candidate newCandidate = new Candidate();
                    newCandidate.setEmail(applicationDto.getEmail());
                    newCandidate.setAppliedDate(LocalDate.now());
                    newCandidate.setStatus(CandidateStatus.APPLIED);
                    return newCandidate;
                });

        // Handle resume upload
        if (applicationDto.getResume() != null && !applicationDto.getResume().isEmpty()) {
            candidate.setResume(applicationDto.getResume().getBytes());
        }

        Candidate savedCandidate = candidateRepository.save(candidate);
        createJobApplication(savedCandidate, applicationDto.getJobId());
        sendJobApplicationEvent(savedCandidate, applicationDto.getJobId());
        return savedCandidate;
    }
    private void sendJobApplicationEvent(Candidate candidate, Long jobId) {
        try {
            log.info("Starting to send job application event for candidate: {}", candidate.getEmail());

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

            JobApplicationEvent event = new JobApplicationEvent(
                    candidate.getEmail(),
                    job.getId(),
                    job.getTitle(),
                    LocalDate.now()
            );

            log.info("Created event: {}", event);
            kafkaTemplate.send("job-application", event);
            log.info("Message sent to Kafka topic: job-application");
        } catch (Exception e) {
            log.error("Error creating job application event", e);
        }
    }
    @Cacheable(value = ALL_CANDIDATES_CACHE, key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Candidate> getCandidatesList(Pageable pageable) {
        Page<Candidate> page = candidateRepository.findAll(pageable);
        return page.getContent();
    }

    @Cacheable(value = CANDIDATES_COUNT_CACHE, key = "'total'")
    public long getTotalCandidatesCount() {
        return candidateRepository.count();
    }

    public Page<Candidate> getAllCandidates(Pageable pageable) {
        List<Candidate> candidates = getCandidatesList(pageable);
        long totalCount = getTotalCandidatesCount();
        return new PageImpl<>(candidates, pageable, totalCount);
    }

    @Cacheable(value = CANDIDATE_BY_ID_CACHE, key = "#id")
    public Candidate getCandidateById(Long id) {
        log.info("Fetching candidate {} from database", id);
        return candidateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
    }

    @Cacheable(value = CANDIDATE_BY_EMAIL_CACHE, key = "#email")
    public Candidate getCandidateByEmail(String email) {
        log.info("Fetching candidate by email {}", email);
        return candidateRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
    }

    public Resource downloadResume(Long candidateId) {
        Candidate candidate = getCandidateById(candidateId);
        if (candidate.getResume() == null) {
            throw new ResourceNotFoundException("Resume not found");
        }

        ByteArrayResource resource = new ByteArrayResource(candidate.getResume()) {
            @Override
            public String getFilename() {
                return "resume_" + candidateId + ".pdf";
            }
        };

        return resource;
    }


    @Cacheable(value = JOB_APPLICATIONS_CACHE, key = "#jobId + '_' + #pageable.pageNumber + '_content'")
    public List<CandidateResponse> getApplicantsContentForJob(Long jobId, Pageable pageable) {
        Page<CandidateResponse> applicants = jobApplicationRepository.findCandidatesByJobId(jobId, pageable)
                .map(CandidateResponse::new);
        return applicants.getContent();
    }

    @Cacheable(value = JOB_APPLICATIONS_CACHE, key = "#jobId + '_total'")
    public int getTotalApplicantsForJob(Long jobId) {
        return jobApplicationRepository.countByJobId(jobId);
    }

    public Page<CandidateResponse> getApplicantsForJob(Long jobId, Pageable pageable) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job with ID " + jobId + " not found");
        }

        List<CandidateResponse> content = getApplicantsContentForJob(jobId, pageable);
        int totalElements = getTotalApplicantsForJob(jobId);

        if (content.isEmpty()) {
            throw new ResourceNotFoundException("No applicants found for job ID " + jobId);
        }

        return new PageImpl<>(content, pageable, totalElements);
    }

    private void createJobApplication(Candidate candidate, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStage("Waiting for response");
        jobApplicationRepository.save(application);
    }
    @Cacheable(value = CANDIDATES_FILTERED_CACHE,
            key = "{#filters.hashCode(), #pageable.pageNumber, #pageable.pageSize}")
    public List<Candidate> getFilteredCandidatesList(CandidateFilterDTO filters, Pageable pageable) {
        Page<Candidate> page = candidateRepositoryCustom.findWithFilters(filters, pageable);
        if (page.isEmpty()) {
            throw new ResourceNotFoundException("No candidates found matching the criteria");
        }
        return page.getContent();
    }

    @Cacheable(value = CANDIDATES_FILTERED_COUNT_CACHE, key = "#filters.hashCode()")
    public long getFilteredCandidatesCount(CandidateFilterDTO filters) {
        return candidateRepositoryCustom.countWithFilters(filters);
    }

    public Page<Candidate> getFilteredCandidates(CandidateFilterDTO filters, Pageable pageable) {
        List<Candidate> content = getFilteredCandidatesList(filters, pageable);
        long totalCount = getFilteredCandidatesCount(filters);
        return new PageImpl<>(content, pageable, totalCount);
    }
    public boolean checkCandidateExists(Long candidateId) {
        return candidateRepository.existsById(candidateId);
    }
    public String getCandidateEmailById(Long candidateId) {
        Candidate candidate = getCandidateById(candidateId);
        return candidate.getEmail();
    }
}