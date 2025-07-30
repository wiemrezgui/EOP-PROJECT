package com.EOP.jobs_service.services;

import com.EOP.jobs_service.DTOs.CandidateApplicationDto;
import com.EOP.jobs_service.DTOs.CandidateResponse;
import com.EOP.jobs_service.exceptions.AppliedJobException;
import com.EOP.jobs_service.exceptions.CandidateNotFoundException;
import com.EOP.jobs_service.exceptions.JobNotFoundException;
import com.EOP.jobs_service.exceptions.ResourceNotFoundException;
import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.models.CandidateStatus;
import com.EOP.jobs_service.models.Job;
import com.EOP.jobs_service.models.JobApplication;
import com.EOP.jobs_service.repositories.CandidateRepository;
import com.EOP.jobs_service.repositories.JobApplicationRepository;
import com.EOP.jobs_service.repositories.JobRepository;
import events.JobApplicationEvent;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateService {
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, JobApplicationEvent> kafkaTemplate;
    // Cache keys
    private static final String ALL_CANDIDATES_CACHE = "all_candidates";
    private static final String CANDIDATE_BY_ID_CACHE = "candidate_";
    private static final String CANDIDATE_BY_EMAIL_CACHE = "candidate_email_";
    private static final String JOB_APPLICATIONS_CACHE = "job_applications";

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
        cacheCandidate(savedCandidate);
        sendJobApplicationEvent(savedCandidate, applicationDto.getJobId());
        return savedCandidate;
    }
    private void sendJobApplicationEvent(Candidate candidate, Long jobId) {
        try {
            log.info("Starting to send job application event for candidate: {}", candidate.getEmail());

            Job job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new JobNotFoundException("Job not found"));

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
    @Cacheable(value = ALL_CANDIDATES_CACHE, key = "#pageable.pageNumber")
    public Page<Candidate> getAllCandidates(Pageable pageable) {
        log.info("Fetching candidates from database (page {})", pageable.getPageNumber());
        return candidateRepository.findAllCandidates(pageable);
    }

    @Cacheable(value = CANDIDATE_BY_ID_CACHE, key = "#id")
    public Candidate getCandidateById(Long id) {
        log.info("Fetching candidate {} from database", id);
        return candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found"));
    }

    @Cacheable(value = CANDIDATE_BY_EMAIL_CACHE, key = "#email")
    public Candidate getCandidateByEmail(String email) {
        log.info("Fetching candidate by email {}", email);
        return candidateRepository.findByEmail(email)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found"));
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

    @Cacheable(value = JOB_APPLICATIONS_CACHE, key = "#jobId + '_' + #pageable.pageNumber")
    public Page<CandidateResponse> getApplicantsForJob(Long jobId, Pageable pageable) {
        return jobApplicationRepository.findCandidatesByJobId(jobId, pageable)
                .map(CandidateResponse::new);
    }


    private void createJobApplication(Candidate candidate, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CandidateNotFoundException("Job not found"));

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJob(job);
        application.setStage("APPLIED");
        jobApplicationRepository.save(application);
    }

    private void cacheCandidate(Candidate candidate) {
        redisTemplate.opsForValue().set(
                CANDIDATE_BY_ID_CACHE + candidate.getId(),
                candidate
        );
        redisTemplate.opsForValue().set(
                CANDIDATE_BY_EMAIL_CACHE + candidate.getEmail(),
                candidate
        );
    }
}