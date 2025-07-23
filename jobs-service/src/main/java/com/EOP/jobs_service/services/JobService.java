package com.EOP.jobs_service.services;

import com.EOP.jobs_service.exception.JobNotFoundException;
import com.EOP.jobs_service.model.Job;
import com.EOP.jobs_service.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import com.EOP.jobs_service.model.JobStatus;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Cache keys
    private static final String ALL_JOBS_CACHE = "all_jobs";
    private static final String JOB_BY_ID_CACHE = "job_";

    @CacheEvict(value = ALL_JOBS_CACHE, allEntries = true)
    public Job createJob(Job job) {
        job.setPostedDate(LocalDate.now());
        job.setStatus(JobStatus.PUBLISHED);
        return jobRepository.save(job);
    }

    @Cacheable(value = ALL_JOBS_CACHE, key = "#pageable.pageNumber")
    public Page<Job> getAllJobs(Pageable pageable) {
        return jobRepository.findAllJobs(pageable);
    }

    @Cacheable(value = JOB_BY_ID_CACHE, key = "#id")
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found with id: " + id));
    }

    @CachePut(value = JOB_BY_ID_CACHE, key = "#job.id")
    @CacheEvict(value = ALL_JOBS_CACHE, allEntries = true)
    public Job updateJob(Job job) {
        if (!jobRepository.existsById(job.getId())) {
            throw new JobNotFoundException("Job not found with id: " + job.getId());
        }
        return jobRepository.save(job);
    }

    @Caching(evict = {
            @CacheEvict(value = JOB_BY_ID_CACHE, key = "#id"),
            @CacheEvict(value = ALL_JOBS_CACHE, allEntries = true)
    })
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    @CachePut(value = JOB_BY_ID_CACHE, key = "#id")
    @CacheEvict(value = ALL_JOBS_CACHE, allEntries = true)
    public Job updateJobStatus(Long id, JobStatus newStatus) {
        Job job = getJobById(id);
        job.setStatus(newStatus);
        return jobRepository.save(job);
    }

    @Cacheable(value = "jobs_by_status", key = "#status.name() + '_' + #pageable.pageNumber")
    public Page<Job> getJobsByStatus(JobStatus status, Pageable pageable) {
        return jobRepository.findByStatus(status, pageable);
    }
}
