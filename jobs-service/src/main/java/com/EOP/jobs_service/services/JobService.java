package com.EOP.jobs_service.services;

import com.EOP.jobs_service.DTOs.JobDTO;
import com.EOP.jobs_service.exceptions.JobNotFoundException;
import com.EOP.jobs_service.exceptions.NoApplicantsFoundException;
import com.EOP.jobs_service.models.Job;
import com.EOP.jobs_service.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import com.EOP.jobs_service.models.JobStatus;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Cache keys
    private static final String JOBS_LIST_CACHE = "jobs-list";
    private static final String JOBS_COUNT_CACHE = "jobs-count";
    private static final String JOB_BY_ID_CACHE = "job_";

    @CacheEvict(value = {JOBS_LIST_CACHE, JOBS_COUNT_CACHE}, allEntries = true)
    public Job createJob(JobDTO jobDTO) {
        Job newJob = new Job();
        newJob.setTitle(jobDTO.getTitle());
        newJob.setDetails(jobDTO.getDetails());
        newJob.setPostedDate(LocalDate.now());
        newJob.setStatus(JobStatus.PUBLISHED);
        return jobRepository.save(newJob);
    }

    @Cacheable(value = JOBS_LIST_CACHE, key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Job> getJobsList(Pageable pageable) {
        Page<Job> page = jobRepository.findAllJobs(pageable);
        return page.getContent();
    }

    // Cache the total count separately
    @Cacheable(value = JOBS_COUNT_CACHE, key = "'total'")
    public long getTotalJobsCount() {
        return jobRepository.count();
    }

    public Page<Job> getAllJobs(Pageable pageable) {
        List<Job> jobs = getJobsList(pageable);
        long totalCount = getTotalJobsCount();
        return new PageImpl<>(jobs, pageable, totalCount);
    }


    @Cacheable(value = JOB_BY_ID_CACHE, key = "#id")
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));
    }

    @CacheEvict(value = {JOBS_LIST_CACHE,JOB_BY_ID_CACHE}, allEntries = true)
    @CachePut(value = JOB_BY_ID_CACHE, key = "#id")
    public Job updateJob(Long id, JobDTO jobDTO) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));
        existingJob.setTitle(jobDTO.getTitle());
        existingJob.setDetails(jobDTO.getDetails());
        return jobRepository.save(existingJob);
    }

    @CacheEvict(value = {JOBS_LIST_CACHE, JOBS_COUNT_CACHE, JOB_BY_ID_CACHE}, allEntries = true)
    public void deleteJob(Long id) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));
        jobRepository.deleteById(id);
    }

    @CacheEvict(value = {JOBS_LIST_CACHE, JOB_BY_ID_CACHE}, allEntries = true)
    @CachePut(value = JOB_BY_ID_CACHE, key = "#id")
    public Job updateJobStatus(Long id, JobStatus newStatus) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found"));
        job.setStatus(newStatus);
        return jobRepository.save(job);
    }

    @Cacheable(value = "jobs_by_status", key = "#status.name() + '_' + #pageable.pageNumber")
    public Page<Job> getJobsByStatus(JobStatus status, Pageable pageable) {
        Page<Job> jobs =jobRepository.findByStatus(status, pageable);
        if (jobs.isEmpty()) {
            throw new NoApplicantsFoundException("No jobs found for this status ");
        }
        return jobs;
    }
}
