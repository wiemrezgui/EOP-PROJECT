package com.EOP.jobs_service.controllers;

import com.EOP.jobs_service.models.Job;
import com.EOP.jobs_service.models.JobStatus;
import com.EOP.jobs_service.services.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Job management APIs")
public class JobController {

    private final JobService jobService;

    @Operation(summary = "Create a new job")
    @PostMapping
    public ResponseEntity<Job> createJob(
            @RequestBody @Valid Job job) {
        return ResponseEntity.ok(jobService.createJob(job));
    }

    @Operation(summary = "Get all jobs with pagination")
    @GetMapping
    public ResponseEntity<Page<Job>> getAllJobs(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {

        return ResponseEntity.ok(jobService.getAllJobs(PageRequest.of(page, size)));
    }

    @Operation(summary = "Get job by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @Operation(summary = "Update job")
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id,

            @RequestBody @Valid Job job) {

        job.setId(id);
        return ResponseEntity.ok(jobService.updateJob(job));
    }

    @Operation(summary = "Delete job")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id) {

        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update job status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Job> updateJobStatus(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id,

            @Parameter(description = "New status", example = "PUBLISHED")
            @RequestParam JobStatus status) {

        return ResponseEntity.ok(jobService.updateJobStatus(id, status));
    }

    @Operation(summary = "Get jobs by status with pagination")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Job>> getJobsByStatus(
            @Parameter(description = "Job status", example = "PUBLISHED")
            @PathVariable JobStatus status,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {

        return ResponseEntity.ok(jobService.getJobsByStatus(status, PageRequest.of(page, size)));
    }
}
