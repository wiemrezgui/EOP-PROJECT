package com.EOP.interview_service.controllers;

import com.EOP.common_lib.common.DTO.ApiResponse;
import com.EOP.common_lib.common.enums.InterviewMode;
import com.EOP.interview_service.DTOs.CreateInterviewRequestDTO;
import com.EOP.interview_service.DTOs.InterviewFilterDTO;
import com.EOP.interview_service.DTOs.InterviewRequestDTO;
import com.EOP.interview_service.enums.InterviewStatus;
import com.EOP.interview_service.models.Interview;
import com.EOP.interview_service.services.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.naming.ServiceUnavailableException;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interview Management", description = "Endpoints for managing interviews")
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Schedule a new interview", description = "Create and schedule a new interview")
    @PostMapping
    public ResponseEntity<ApiResponse<Interview>> createInterview(
            @RequestBody @Valid CreateInterviewRequestDTO request) throws ServiceUnavailableException {

        log.info("Creating new interview for candidate: {}", request.getCandidateID());

        Interview interview = interviewService.createInterview(request);

        ApiResponse<Interview> apiResponse = ApiResponse.success(
                interview,
                "Interview scheduled successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Operation(summary = "Get all interviews", description = "Retrieve paginated list of all interviews")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Interview>>> getAllInterviews(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Items per page", example = "10")
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledDate").ascending());
        Page<Interview> interviews = interviewService.getAllInterviews(pageable);

        ApiResponse<Page<Interview>> apiResponse = ApiResponse.success(
                interviews,
                "Interviews retrieved successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get interview by ID", description = "Retrieve details of a specific interview")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Interview>> getInterviewById(
            @Parameter(description = "Interview ID", example = "1")
            @PathVariable Long id) {

        Interview interview = interviewService.getInterviewById(id);
        ApiResponse<Interview> apiResponse = ApiResponse.success(
                interview,
                "Interview retrieved successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get interviews by candidate", description = "Retrieve paginated interviews for a specific candidate")
    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<ApiResponse<Page<Interview>>> getInterviewsByCandidate(
            @Parameter(description = "Candidate ID", example = "123")
            @PathVariable Long candidateId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Interview> interviews = interviewService.getInterviewsByCandidate(candidateId, pageable);

        ApiResponse<Page<Interview>> apiResponse = ApiResponse.success(
                interviews,
                "Candidate interviews retrieved successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }
    @Operation(summary = "Get interviews by status", description = "Retrieve paginated interviews for a specific status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<Interview>>> getInterviewsByStatus(
            @Parameter(description = "Status", example = "CANCELLED")
            @PathVariable String status,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Interview> interviews = interviewService.getInterviewsByStatus(InterviewStatus.valueOf(status.toUpperCase()), pageable);

        ApiResponse<Page<Interview>> apiResponse = ApiResponse.success(
                interviews,
                "Candidate interviews retrieved successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get interviews by mode", description = "Retrieve paginated interviews for a specific mode")
    @GetMapping("/mode/{mode}")
    public ResponseEntity<ApiResponse<Page<Interview>>> getInterviewsByMode(
            @Parameter(description = "Mode", example = "IN_PERSON")
            @PathVariable String mode,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Interview> interviews = interviewService.getInterviewsByMode(InterviewMode.valueOf(mode.toUpperCase()), pageable);

        ApiResponse<Page<Interview>> apiResponse = ApiResponse.success(
                interviews,
                "Candidate interviews retrieved successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Filter interviews", description = "Retrieve filtered interviews")
    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<Page<Interview>>> getFilteredInterviews(
            @RequestBody InterviewFilterDTO request,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Interview> interviews = interviewService.getFilteredInterviews(request, pageable);

        ApiResponse<Page<Interview>> apiResponse = ApiResponse.success(
                interviews,
                "Filtered interviews retrieved successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Update interview details", description = "Update the details of an existing interview")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Interview>> updateInterview(
            @Parameter(description = "Interview ID", example = "1")
            @PathVariable Long id,

            @RequestBody @Valid InterviewRequestDTO request) throws ServiceUnavailableException {

        log.info("Updating interview with ID: {}", id);

        Interview updatedInterview = interviewService.updateInterview(id, request);

        ApiResponse<Interview> apiResponse = ApiResponse.success(
                updatedInterview,
                "Interview updated successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Update interview status", description = "Update the status of an interview")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Interview>> updateInterviewStatus(
            @Parameter(description = "Interview ID", example = "1")
            @PathVariable Long id,

            @RequestBody @Valid InterviewStatus status) {

        log.info("Updating status for interview ID {} to {}", id, status);

        Interview updatedInterview = interviewService.updateStatus(id, status);

        ApiResponse<Interview> apiResponse = ApiResponse.success(
                updatedInterview,
                "Interview status updated successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/{id}/feedback")
    @Operation(summary = "Submit interview feedback")
    public ResponseEntity<ApiResponse<Interview>> updateInterviewFeedback(
            @Parameter(description = "Interview ID") @PathVariable Long id,
            @RequestBody @Valid String feedback) {
        Interview updatedInterview =interviewService.updateFeedback(id, feedback);
        ApiResponse<Interview> apiResponse = ApiResponse.success(
                updatedInterview,
                "Feedback updated successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Delete interview", description = "Delete an interview by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelInterview(
            @Parameter(description = "Interview ID", example = "1")
            @PathVariable Long id ,
            @RequestParam(required = false) String cancellationReason ) {

        log.info("Deleting interview with ID: {}", id);

        interviewService.cancelInterview(id,cancellationReason);

        ApiResponse<Void> apiResponse = ApiResponse.success(
                null,
                "Interview cancelled successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

}
