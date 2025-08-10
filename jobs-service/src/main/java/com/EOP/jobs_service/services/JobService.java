package com.EOP.jobs_service.services;

import com.EOP.common_lib.common.exceptions.ResourceNotFoundException;
import com.EOP.jobs_service.DTOs.JobDTO;
import com.EOP.jobs_service.DTOs.JobFilterDTO;
import com.EOP.jobs_service.interfaces.JobRepositoryCustom;
import com.EOP.jobs_service.models.Job;
import com.EOP.jobs_service.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

import com.EOP.jobs_service.enums.JobStatus;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;
    private final JobRepositoryCustom jobRepositoryCustom;
    // Cache keys
    private static final String JOBS_LIST_CACHE = "jobs-list";
    private static final String JOBS_COUNT_CACHE = "jobs-count";
    private static final String JOB_BY_ID_CACHE = "job_";
    private static final String JOBS_BY_STATUS_CACHE = "jobs_by_status";
    private static final String JOBS_BY_STATUS_COUNT_CACHE = "jobs_by_status_count";

    private static final String JOBS_FILTERED_CACHE = "filtered_jobs";
    private static final String JOBS_FILTERED_COUNT_CACHE = "filtered_jobs_count";

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
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    @CacheEvict(value = {JOBS_LIST_CACHE,JOB_BY_ID_CACHE}, allEntries = true)
    @CachePut(value = JOB_BY_ID_CACHE, key = "#id")
    public Job updateJob(Long id, JobDTO jobDTO) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        existingJob.setTitle(jobDTO.getTitle());
        existingJob.setDetails(jobDTO.getDetails());
        return jobRepository.save(existingJob);
    }

    @CacheEvict(value = {JOBS_LIST_CACHE, JOBS_COUNT_CACHE, JOB_BY_ID_CACHE}, allEntries = true)
    public void deleteJob(Long id) {
        Job existingJob = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
        jobRepository.deleteById(id);
    }

    @CacheEvict(value = {JOBS_LIST_CACHE, JOB_BY_ID_CACHE}, allEntries = true)
    @CachePut(value = JOB_BY_ID_CACHE, key = "#id")
    public Job updateJobStatus(Long id, JobStatus newStatus) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setStatus(newStatus);
        return jobRepository.save(job);
    }

    @Cacheable(value = JOBS_BY_STATUS_CACHE,
            key = "#status.name() + '_page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Job> getJobsListByStatus(JobStatus status, Pageable pageable) {
        Page<Job> page = jobRepository.findByStatus(status, pageable);
        if (page.isEmpty()) {
            throw new ResourceNotFoundException("No jobs found for status: " + status);
        }
        return page.getContent();
    }

    @Cacheable(value = JOBS_BY_STATUS_COUNT_CACHE, key = "#status.name()")
    public long getTotalJobsCountByStatus(JobStatus status) {
        return jobRepository.countByStatus(status);
    }

    public Page<Job> getJobsByStatus(JobStatus status, Pageable pageable) {
        List<Job> content = getJobsListByStatus(status, pageable);
        long totalCount = getTotalJobsCountByStatus(status);
        return new PageImpl<>(content, pageable, totalCount);
    }
    @Cacheable(value = JOBS_FILTERED_CACHE,
            key = "{#filters.hashCode(), #pageable.pageNumber, #pageable.pageSize}")
    public List<Job> getFilteredJobsList(JobFilterDTO filters, Pageable pageable) {
        Page<Job> page = jobRepositoryCustom.findWithFilters(filters, pageable);
        if (page.isEmpty()) {
            throw new ResourceNotFoundException("No jobs found matching the criteria");
        }
        return page.getContent();
    }

    @Cacheable(value = JOBS_FILTERED_COUNT_CACHE, key = "#filters.hashCode()")
    public long getFilteredJobsCount(JobFilterDTO filters) {
        return jobRepositoryCustom.countWithFilters(filters);
    }

    public Page<Job> getFilteredJobs(JobFilterDTO filters, Pageable pageable) {
        List<Job> content = getFilteredJobsList(filters, pageable);
        long totalCount = getFilteredJobsCount(filters);
        return new PageImpl<>(content, pageable, totalCount);
    }
    public String getJobTitleById(Long id) {
        Job job = getJobById(id);
        return job.getTitle();
    }
}
