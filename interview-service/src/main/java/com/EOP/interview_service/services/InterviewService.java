package com.EOP.interview_service.services;

import com.EOP.common_lib.common.enums.InterviewMode;
import com.EOP.common_lib.common.exceptions.ResourceNotFoundException;
import com.EOP.common_lib.events.InterviewCancelledEvent;
import com.EOP.common_lib.events.InterviewCreatedEvent;
import com.EOP.common_lib.events.InterviewUpdatedEvent;
import com.EOP.interview_service.DTOs.CreateInterviewRequestDTO;
import com.EOP.interview_service.DTOs.InterviewFilterDTO;
import com.EOP.interview_service.DTOs.InterviewRequestDTO;
import com.EOP.interview_service.DTOs.MeetingDetailsDTO;
import com.EOP.interview_service.clients.AuthServiceClient;
import com.EOP.interview_service.clients.JobsServiceClient;
import com.EOP.interview_service.enums.InterviewStatus;
import com.EOP.interview_service.exceptions.InvalidRequestException;
import com.EOP.interview_service.models.Interview;
import com.EOP.interview_service.repositories.InterviewRepository;
import com.EOP.interview_service.repositories.InterviewRepositoryImpl;
import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.naming.ServiceUnavailableException;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {
    private final AuthServiceClient authClient;
    private final JobsServiceClient jobsClient;
    private final InterviewRepository interviewRepository;
    private final GoogleMeetService googleMeetService;
    private final InterviewRepositoryImpl interviewRepositoryImpl;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    //cache keys
    public static final String INTERVIEWS_LIST_CACHE = "interviews-list";
    public static final String INTERVIEWS_COUNT_CACHE = "interviews-count";
    public static final String INTERVIEW_BY_ID_CACHE = "interview_";
    public static final String INTERVIEWS_BY_CANDIDATE_CACHE = "interviews_by_candidate";
    public static final String INTERVIEWS_BY_STATUS_CACHE = "interviews_by_status";
    public static final String INTERVIEWS_BY_MODE_CACHE = "interviews_by_mode";

    @CacheEvict(value = {INTERVIEWS_LIST_CACHE, INTERVIEWS_COUNT_CACHE, INTERVIEWS_BY_CANDIDATE_CACHE, INTERVIEWS_BY_STATUS_CACHE,INTERVIEWS_BY_MODE_CACHE}, allEntries = true)
    @Transactional
    public Interview createInterview(CreateInterviewRequestDTO request) throws ServiceUnavailableException {
        validateExistance(request.getCandidateID(), request.getUserEmail(),request.getJobID());
        validateInterviewMode(request);
        validateInterviewDate(request);
        Interview interview = new Interview();
        interview.setScheduledDate(request.getScheduledDate());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setDescription(request.getDescription());
        interview.setStatus(InterviewStatus.SCHEDULED);
        interview.setCandidateID(request.getCandidateID());
        interview.setJobID(request.getJobID());
        interview.setUserEmail(request.getUserEmail());
        interview.setMeetingTitle(request.getMeetingTitle());
        interview.setMode(request.getMode());
        if (request.getMode() == InterviewMode.ONLINE) {
            handleOnlineInterview(interview);
        } else { // PRESENTIAL
            interview.setLocation(request.getLocation());
            interview.setMeetingLink(null); // Clear link if exists
        }
        Interview addedInterview= interviewRepository.save(interview);
        sendInterviewCreatedEvent(addedInterview);
        return addedInterview;
    }

    private void sendInterviewCreatedEvent(Interview interview) {
        try {
            log.info("Starting to send interview event ");

            String jobTitle=this.getJobTitle(interview.getJobID());
            String candidateEmail=this.jobsClient.getCandidateEmailById(interview.getCandidateID());
            InterviewCreatedEvent event = new InterviewCreatedEvent(
                    interview.getUserEmail(),
                    jobTitle,
                    interview.getMeetingTitle(),
                    interview.getScheduledDate(),
                    interview.getScheduledTime(),
                    interview.getMode(),
                    interview.getLocation(),
                    interview.getMeetingLink(),
                    interview.getDescription(),
                    candidateEmail,
                    interview.getDurationMinutes()
            );

            log.info("Created event: {}", event);
            kafkaTemplate.send("interview-created", event);
            log.info("Message sent to Kafka topic: interview-created");
        } catch (Exception e) {
            log.error("Error creating interview event", e);
        }
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

    @Cacheable(value = INTERVIEWS_BY_MODE_CACHE, key = "#mode.name() + '_page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize")
    public List<Interview> getInterviewsListByMode(InterviewMode mode, Pageable pageable) {
        Page<Interview> page = interviewRepository.findByMode(mode, pageable);
        return page.getContent();
    }

    @Cacheable(value = INTERVIEWS_BY_MODE_CACHE + "_count", key = "#mode.name()")
    public long getInterviewsCountByMode(InterviewMode mode) {
        return interviewRepository.countByMode(mode);
    }

    public Page<Interview> getInterviewsByMode(InterviewMode mode, Pageable pageable) {
        List<Interview> interviews = getInterviewsListByMode(mode, pageable);
        if (interviews.isEmpty()) {
            throw new ResourceNotFoundException("No interviews found");
        }
        long totalCount = getInterviewsCountByMode(mode);
        return new PageImpl<>(interviews, pageable, totalCount);
    }

    @CacheEvict(value = {INTERVIEWS_LIST_CACHE, INTERVIEWS_BY_CANDIDATE_CACHE, INTERVIEWS_BY_STATUS_CACHE,INTERVIEWS_BY_MODE_CACHE}, allEntries = true)
    @CachePut(value = INTERVIEW_BY_ID_CACHE, key = "#id")
    @Transactional
    public Interview updateInterview(Long id, InterviewRequestDTO request) throws ServiceUnavailableException {
        Interview interview = getInterviewById(id);
        // Track changes for notification
        boolean timeChanged = !interview.getScheduledDate().equals(request.getScheduledDate()) ||
                !interview.getScheduledTime().equals(request.getScheduledTime());
        boolean modeChanged = interview.getMode() != request.getMode();
        boolean locationChanged = !interview.getLocation().equals(request.getLocation());
        interview.setScheduledDate(request.getScheduledDate());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setMode(request.getMode());
        if (request.getMode() == InterviewMode.ONLINE) {
            handleOnlineInterview(interview);
        } else { // PRESENTIAL
            interview.setLocation(request.getLocation());
            interview.setMeetingLink(null); // Clear link if exists
        }
        interview.setDurationMinutes(request.getDurationMinutes());
        interview.setDescription(request.getDescription());
        Interview updatedInterview = interviewRepository.save(interview);
        sendInterviewUpdatedEvent(updatedInterview, timeChanged, modeChanged,locationChanged);
        return updatedInterview;
    }
    private void sendInterviewUpdatedEvent(Interview interview, boolean timeChanged, boolean modeChanged,boolean locationChanged) {
        try {
            String jobTitle = jobsClient.getJobTitleById(interview.getJobID());
            String candidateEmail = jobsClient.getCandidateEmailById(interview.getCandidateID());

            InterviewUpdatedEvent event = InterviewUpdatedEvent.builder()
                    .interviewerEmail(interview.getUserEmail())
                    .candidateEmail(candidateEmail)
                    .jobTitle(jobTitle)
                    .previousDate(interview.getScheduledDate())
                    .previousTime(interview.getScheduledTime())
                    .newDate(interview.getScheduledDate())
                    .newTime(interview.getScheduledTime())
                    .mode(interview.getMode())
                    .location(interview.getLocation())
                    .meetingLink(interview.getMeetingLink())
                    .description(interview.getDescription())
                    .timeChanged(timeChanged)
                    .modeChanged(modeChanged)
                    .locationChanged(locationChanged)
                    .build();

            kafkaTemplate.send("interview-updated", event);
            log.info("Sent interview updated event for interview ID: {}", interview.getId());
        } catch (Exception e) {
            log.error("Failed to send interview updated event", e);
        }
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
    public void cancelInterview(Long id,String cancellationReason) {
        if (!interviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Interview not found with id: " + id);
        }
        Interview interview = getInterviewById(id);
        interview.setStatus(InterviewStatus.CANCELLED);
        interviewRepository.save(interview);

        sendInterviewCancelledEvent(interview, cancellationReason);
    }
    private void sendInterviewCancelledEvent(Interview interview, String cancellationReason) {
        try {
            String jobTitle = jobsClient.getJobTitleById(interview.getJobID());
            String candidateEmail = jobsClient.getCandidateEmailById(interview.getCandidateID());

            InterviewCancelledEvent event = InterviewCancelledEvent.builder()
                    .interviewerEmail(interview.getUserEmail())
                    .candidateEmail(candidateEmail)
                    .jobTitle(jobTitle)
                    .scheduledDate(interview.getScheduledDate())
                    .scheduledTime(interview.getScheduledTime())
                    .cancellationReason(cancellationReason)
                    .build();

            kafkaTemplate.send("interview-cancelled", event);
            log.info("Sent interview cancelled event for interview ID: {}", interview.getId());
        } catch (Exception e) {
            log.error("Failed to send interview cancelled event", e);
        }
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

    private String generateGoogleMeetLink(Interview interview) {
        // Combine date and time for start time
        LocalDateTime startDateTime = LocalDateTime.of(
                interview.getScheduledDate(),
                interview.getScheduledTime()
        );

        // Calculate end time based on duration
        LocalDateTime endDateTime = startDateTime.plusMinutes(interview.getDurationMinutes());

        // Generate Google Meet link
        MeetingDetailsDTO meetingDetails = googleMeetService.createMeeting(
                interview.getMeetingTitle(),
                startDateTime,
                endDateTime
        );

        return meetingDetails.getMeetingLink();

    }

    private void validateInterviewMode(CreateInterviewRequestDTO request) {
        if (request.getMode() == InterviewMode.ONLINE) {
            if (StringUtils.isBlank(request.getMeetingTitle())) {
                throw new InvalidRequestException("Meeting title is required for online interviews");
            }
        } else { // PRESENTIAL
            if (StringUtils.isBlank(request.getLocation())) {
                throw new InvalidRequestException("Location is required for presential interviews");
            }
        }
    }
    private void validateInterviewDate(CreateInterviewRequestDTO request) {
        if (request.getScheduledDate().isBefore(ChronoLocalDate.from(LocalDateTime.now()))) {
            throw new IllegalArgumentException("Interview date must be in the future");
        }
    }
    private void handleOnlineInterview(Interview interview) throws ServiceUnavailableException {
        try {
            String meetingLink = generateGoogleMeetLink(interview);
            interview.setMeetingLink(meetingLink);
            log.info("Google Meet link generated for interview ID: {}", interview.getId());
        } catch (Exception e) {
            log.error("Failed to generate Google Meet link for interview with candidate {}: {}",
                    interview.getCandidateID(), e.getMessage());
            throw new ServiceUnavailableException("Failed to generate meeting link. Please try again later.");
        }
    }
    public Page<Interview> getFilteredInterviews(InterviewFilterDTO filters, Pageable pageable) {
        Page<Interview> interviews = interviewRepositoryImpl.findWithFilters(filters, pageable);
        if (interviews.isEmpty()) {
            throw new ResourceNotFoundException("No interviews found");
        }
        return interviews;
    }
    public String getJobTitle(Long id) {
        return  this.jobsClient.getJobTitleById(id);
    }
}
