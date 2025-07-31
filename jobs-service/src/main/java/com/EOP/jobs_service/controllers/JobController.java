package com.EOP.jobs_service.controllers;

import com.EOP.jobs_service.DTOs.JobDTO;
import com.EOP.jobs_service.models.ApiResponse;
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
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<Job>> createJob(
            @RequestBody JobDTO job) {
        Job newJob = jobService.createJob(job);
        ApiResponse<Job> apiResponse = ApiResponse.success(
                newJob,"Job added successfully!"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Operation(summary = "Get all jobs with pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Job>>> getAllJobs(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {
        Page<Job> allJobs=jobService.getAllJobs(PageRequest.of(page, size));
        ApiResponse<Page<Job>> apiResponse = ApiResponse.success(
                allJobs,"Jobs retrieved successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get job by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getJobById(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id) {
        Job job = jobService.getJobById(id);
        ApiResponse<Job> apiResponse = ApiResponse.success(
                job,"Job retrieved successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Update job")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> updateJob(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid JobDTO jobDTO) {
        Job job = jobService.updateJob(id, jobDTO);
        ApiResponse<Job> apiResponse = ApiResponse.success(
                job,"Job modified successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Delete job")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id) {

        jobService.deleteJob(id);
        ApiResponse<Void> apiResponse = ApiResponse.success(null,"Job deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Update job status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Job>> updateJobStatus(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long id,

            @Parameter(description = "New status", example = "PUBLISHED")
            @RequestParam JobStatus status) {
        Job job = jobService.updateJobStatus(id, status);
        ApiResponse<Job> apiResponse = ApiResponse.success(
                job,"Job status modified successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get jobs by status with pagination")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<Job>>> getJobsByStatus(
            @Parameter(description = "Job status", example = "PUBLISHED")
            @PathVariable JobStatus status,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {
        Page<Job> jobs=jobService.getJobsByStatus(status, PageRequest.of(page, size));
        ApiResponse<Page<Job>> apiResponse = ApiResponse.success(
                jobs,"Jobs retrieved successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }
}
