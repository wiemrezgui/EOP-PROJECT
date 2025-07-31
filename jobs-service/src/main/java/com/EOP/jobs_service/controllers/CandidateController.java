package com.EOP.jobs_service.controllers;

import com.EOP.jobs_service.DTOs.CandidateApplicationDto;
import com.EOP.jobs_service.DTOs.CandidateResponse;
import com.EOP.jobs_service.exceptions.InvalidRequestException;
import com.EOP.jobs_service.models.ApiResponse;
import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.services.CandidateService;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;

@AllArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/jobs/candidates")
@Tag(name = "Candidates", description = "Candidates management APIs")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Apply for a job", description = "Submit a job application")
    @PostMapping(value = "/apply", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ApiResponse<CandidateResponse>> applyForJob(
            @RequestParam
            @NotNull(message = "Job ID is required")
            @Positive(message = "Job ID must be positive") Long jobId,
            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            @RequestParam String email,
            @NotBlank(message = "Resume is required")
            @RequestPart MultipartFile resume) throws IOException {

        log.info("Received job application: jobId={}, email={}, hasResume={}",
                jobId, email, resume != null && !resume.isEmpty());

        // Validate file if provided
        if (resume != null && !resume.isEmpty()) {
            validateResumeFile(resume);
        }

        try {
            CandidateApplicationDto dto = new CandidateApplicationDto();
            dto.setEmail(email.trim().toLowerCase());
            dto.setJobId(jobId);
            dto.setResume(resume);

            Candidate candidate = candidateService.applyForJob(dto);
            CandidateResponse response = new CandidateResponse(candidate);

            ApiResponse<CandidateResponse> apiResponse = ApiResponse.success(
                    response,
                    "Application submitted successfully!"
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);

        } catch (IOException e) {
            log.error("File processing error for email {}: {}", email, e.getMessage());
            throw new InvalidRequestException("Failed to process resume file. Please try again.");
        }
    }

    @Operation(summary = "Get all candidates", description = "Retrieve paginated list of candidates")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getAllCandidates(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedDate").descending());
        Page<CandidateResponse> candidates = candidateService.getAllCandidates(pageable)
                .map(CandidateResponse::new);

        ApiResponse<Page<CandidateResponse>> response = ApiResponse.success(
                candidates,
                "Candidates retrieved successfully"
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get candidate by ID", description = "Retrieve a specific candidate's details")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(
            @Parameter(description = "Candidate ID", example = "1")
            @PathVariable Long id) {

        Candidate candidate = candidateService.getCandidateById(id);
        CandidateResponse response = new CandidateResponse(candidate);

        ApiResponse<CandidateResponse> apiResponse = ApiResponse.success(
                response,
                "Candidate found successfully"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Get candidate by email", description = "Retrieve a candidate by their email address")
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateByEmail(
            @Parameter(description = "Candidate email", example = "john.doe@example.com")
            @PathVariable
            @Email(message = "Enter valid email")
            String email) {

        CandidateResponse response= new CandidateResponse(candidateService.getCandidateByEmail(email));
        ApiResponse<CandidateResponse> apiResponse = ApiResponse.success(
                response,
                "Candidate retrieved by email successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }

    @Operation(summary = "Download resume", description = "Download a candidate's resume PDF")
    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadResume(
            @Parameter(description = "Candidate ID", example = "1")
            @PathVariable Long id) {

        Resource resource = candidateService.downloadResume(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @Operation(summary = "Get job applicants", description = "Retrieve paginated list of applicants for a job")
    @GetMapping("/job/{jobId}/applicants")
    public ResponseEntity<ApiResponse<Page<CandidateResponse>>> getApplicantsForJob(
            @Parameter(description = "Job ID", example = "1")
            @NotNull(message = "Job ID is required")
            @Positive(message = "Job ID must be positive")
            @PathVariable Long jobId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CandidateResponse> response = candidateService.getApplicantsForJob(jobId, pageable);
        ApiResponse<Page<CandidateResponse>> apiResponse = ApiResponse.success(
                response,
                "All applicants for the job retrieved successfully"
        );
        return ResponseEntity.ok(apiResponse);
    }
    private void validateResumeFile(MultipartFile file) {
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidRequestException("Resume file size must not exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") &&
                !contentType.startsWith("image/") &&
                !contentType.contains("document"))) {
            throw new InvalidRequestException("Resume must be a PDF, image, or document file");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new InvalidRequestException("Resume filename cannot be empty");
        }
    }
}