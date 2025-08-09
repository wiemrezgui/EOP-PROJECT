package com.EOP.interview_service.services;

import com.EOP.common_lib.common.exceptions.ResourceNotFoundException;
import com.EOP.interview_service.DTOs.CreateInterviewRequestDTO;
import com.EOP.interview_service.DTOs.InterviewRequestDTO;
import com.EOP.interview_service.clients.AuthServiceClient;
import com.EOP.interview_service.clients.JobsServiceClient;
import com.EOP.interview_service.enums.InterviewStatus;
import com.EOP.interview_service.models.Interview;
import com.EOP.interview_service.repositories.InterviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {
    private final AuthServiceClient authClient;
    private final JobsServiceClient jobsClient;
    private final InterviewRepository interviewRepository;

    //cache keys
    public static final String INTERVIEWS_LIST_CACHE = "interviews-list";
    public static final String INTERVIEWS_COUNT_CACHE = "interviews-count";
    public static final String INTERVIEW_BY_ID_CACHE = "interview_";
    public static final String INTERVIEWS_BY_CANDIDATE_CACHE = "interviews_by_candidate";
    public static final String INTERVIEWS_BY_STATUS_CACHE = "interviews_by_status";

    @CacheEvict(value = {INTERVIEWS_LIST_CACHE, INTERVIEWS_COUNT_CACHE, INTERVIEWS_BY_CANDIDATE_CACHE, INTERVIEWS_BY_STATUS_CACHE}, allEntries = true)
    @Transactional
    public Interview createInterview(CreateInterviewRequestDTO request) {
        validateExistance(request.getCandidateID(), request.getUserEmail(),request.getJobID());
        Interview interview = new Interview();
        interview.setScheduledDate(request.getScheduledDate());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setDescription(request.getDescription());
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setCandidateID(request.getCandidateID());
        interview.setJobID(request.getJobID());
        interview.setUserEmail(request.getUserEmail());
        return interviewRepository.save(interview);
    }
    @Cacheable(value = INTERVIEWS_LIST_CACHE, key = "'page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Interview> getInterviewsList(Pageable pageable) {
        Page<Interview> page = interviewRepository.findAll(pageable);
        return page.getContent();
    }

    @Cacheable(value = INTERVIEWS_COUNT_CACHE, key = "'total'")
    public long getTotalInterviewsCount() {
        return interviewRepository.count();
    }

    public Page<Interview> getAllInterviews(Pageable pageable) {
        List<Interview> interviews = getInterviewsList(pageable);
        if (interviews.isEmpty()) {
            throw new ResourceNotFoundException("No interviews found");
        }
        long totalCount = getTotalInterviewsCount();
        return new PageImpl<>(interviews, pageable, totalCount);
    }

    @Cacheable(value = INTERVIEW_BY_ID_CACHE, key = "#id")
    public Interview getInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + id));
    }
    @Cacheable(value = INTERVIEWS_BY_CANDIDATE_CACHE, key = "#candidateId + '_page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Interview> getInterviewsListByCandidate(Long candidateId, Pageable pageable) {
        if (!jobsClient.checkCandidateExists(candidateId)) {
            throw new ResourceNotFoundException("Candidate not found with id: " + candidateId);
        }
        Page<Interview> page = interviewRepository.findByCandidateID(candidateId, pageable);
        return page.getContent();
    }

    @Cacheable(value = INTERVIEWS_BY_CANDIDATE_CACHE + "_count", key = "#candidateId")
    public long getInterviewsCountByCandidate(Long candidateId) {
        return interviewRepository.countByCandidateID(candidateId);
    }

    public Page<Interview> getInterviewsByCandidate(Long candidateId, Pageable pageable) {
        List<Interview> interviews = getInterviewsListByCandidate(candidateId, pageable);
        if (interviews.isEmpty()) {
            throw new ResourceNotFoundException("No interviews found");
        }
        long totalCount = getInterviewsCountByCandidate(candidateId);
        return new PageImpl<>(interviews, pageable, totalCount);
    }

    @Cacheable(value = INTERVIEWS_BY_STATUS_CACHE, key = "#status.name() + '_page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Interview> getInterviewsListByStatus(InterviewStatus status, Pageable pageable) {
        Page<Interview> page = interviewRepository.findByStatus(status, pageable);
        return page.getContent();
    }

    @Cacheable(value = INTERVIEWS_BY_STATUS_CACHE + "_count", key = "#status.name()")
    public long getInterviewsCountByStatus(InterviewStatus status) {
        return interviewRepository.countByStatus(status);
    }

    public Page<Interview> getInterviewsByStatus(InterviewStatus status, Pageable pageable) {
        List<Interview> interviews = getInterviewsListByStatus(status, pageable);
        if (interviews.isEmpty()) {
            throw new ResourceNotFoundException("No interviews found");
        }
        long totalCount = getInterviewsCountByStatus(status);
        return new PageImpl<>(interviews, pageable, totalCount);
    }

    @CacheEvict(value = {INTERVIEWS_LIST_CACHE, INTERVIEWS_BY_CANDIDATE_CACHE, INTERVIEWS_BY_STATUS_CACHE}, allEntries = true)
    @CachePut(value = INTERVIEW_BY_ID_CACHE, key = "#id")
    @Transactional
    public Interview updateInterview(Long id, InterviewRequestDTO request) {
        Interview interview = getInterviewById(id);
        interview.setScheduledDate(request.getScheduledDate());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setDescription(request.getDescription());
        return interviewRepository.save(interview);
    }

    @CacheEvict(value = {INTERVIEWS_LIST_CACHE, INTERVIEWS_BY_STATUS_CACHE}, allEntries = true)
    @CachePut(value = INTERVIEW_BY_ID_CACHE, key = "#id")
    @Transactional
    public Interview updateStatus(Long id, InterviewStatus status) {
        Interview interview = getInterviewById(id);
        interview.setStatus(status);
        return interviewRepository.save(interview);
    }
    @CacheEvict(value = INTERVIEWS_BY_STATUS_CACHE, allEntries = true)
    @CachePut(value = INTERVIEW_BY_ID_CACHE, key = "#id")
    @Transactional
    public Interview updateFeedback(Long id, String feedback) {
        Interview interview = getInterviewById(id);
        interview.setFeedback(feedback);
        return interviewRepository.save(interview);
    }
    public void deleteInterview(Long id) {
        if (!interviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Interview not found with id: " + id);
        }
        interviewRepository.deleteById(id);
    }
    private void validateExistance(Long candidateId, String userEmail,Long jobId) {
        if (!jobsClient.checkCandidateExists(candidateId)) {
            throw new ResourceNotFoundException("Candidate not found with id: " + candidateId);
        }
        if (!jobsClient.validateJobApplication(jobId, candidateId)) {
            throw new ResourceNotFoundException("Job application not found with id: " + jobId +" and candidate id: "+candidateId);
        }
        if (!authClient.checkUserExists(userEmail)) {
            throw new ResourceNotFoundException("User not found with UUID: " + userEmail);
        }
    }

}
