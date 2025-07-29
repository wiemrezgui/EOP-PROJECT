package com.EOP.jobs_service.controller;

import com.EOP.jobs_service.DTO.CandidateApplicationDto;
import com.EOP.jobs_service.DTO.CandidateApplicationRequest;
import com.EOP.jobs_service.DTO.CandidateResponse;
import com.EOP.jobs_service.model.Candidate;
import com.EOP.jobs_service.services.CandidateService;
import lombok.AllArgsConstructor;
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
import jakarta.validation.Valid;
import java.io.IOException;

@AllArgsConstructor
@RestController
@RequestMapping("/api/jobs/candidates")
@Tag(name = "Candidates", description = "Candidates management APIs")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Apply for a job", description = "Submit a job application (creates candidate if new)")
    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateResponse> applyForJob(
            @Valid @RequestPart CandidateApplicationRequest applicationRequest,
            @RequestPart(required = false) MultipartFile resume) throws IOException {

        CandidateApplicationDto dto = new CandidateApplicationDto();
        dto.setEmail(applicationRequest.getEmail());
        dto.setJobId(applicationRequest.getJobId());
        dto.setResume(resume);

        Candidate candidate = candidateService.applyForJob(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CandidateResponse(candidate));
    }

    @Operation(summary = "Get all candidates", description = "Retrieve paginated list of candidates")
    @GetMapping
    public ResponseEntity<Page<CandidateResponse>> getAllCandidates(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedDate").descending());
        Page<CandidateResponse> response = candidateService.getAllCandidates(pageable)
                .map(CandidateResponse::new);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get candidate by ID", description = "Retrieve a specific candidate's details")
    @GetMapping("/{id}")
    public ResponseEntity<CandidateResponse> getCandidateById(
            @Parameter(description = "Candidate ID", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(new CandidateResponse(candidateService.getCandidateById(id)));
    }

    @Operation(summary = "Get candidate by email", description = "Retrieve a candidate by their email address")
    @GetMapping("/email/{email}")
    public ResponseEntity<CandidateResponse> getCandidateByEmail(
            @Parameter(description = "Candidate email", example = "john.doe@example.com")
            @PathVariable String email) {

        return ResponseEntity.ok(new CandidateResponse(candidateService.getCandidateByEmail(email)));
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
    public ResponseEntity<Page<CandidateResponse>> getApplicantsForJob(
            @Parameter(description = "Job ID", example = "1")
            @PathVariable Long jobId,

            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Items per page", example = "15")
            @RequestParam(defaultValue = "15") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CandidateResponse> response = candidateService.getApplicantsForJob(jobId, pageable)
                .map(CandidateResponse::new);
        return ResponseEntity.ok(response);
    }
}