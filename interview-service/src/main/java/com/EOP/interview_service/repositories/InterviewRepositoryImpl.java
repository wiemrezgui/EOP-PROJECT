package com.EOP.interview_service.repositories;

import com.EOP.interview_service.DTOs.InterviewFilterDTO;
import com.EOP.interview_service.interfaces.InterviewRepositoryCustom;
import com.EOP.interview_service.models.Interview;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InterviewRepositoryImpl implements InterviewRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<Interview> findWithFilters(InterviewFilterDTO filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Interview> query = cb.createQuery(Interview.class);
        Root<Interview> root = query.from(Interview.class);

        List<Predicate> predicates = buildPredicates(filters, cb, root);
        log.debug("Final predicates count: {}", predicates.size());

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.asc(root.get("scheduledTime")));

        TypedQuery<Interview> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Interview> result = typedQuery.getResultList();
        log.debug("Found {} interviews", result.size());

        return new PageImpl<>(result, pageable, countWithFilters(filters));
    }
    @Override
    public long countWithFilters(InterviewFilterDTO filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Interview> root = query.from(Interview.class);

        List<Predicate> predicates = buildPredicates(filters, cb, root);
        query.select(cb.count(root));
        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }
    private List<Predicate> buildPredicates(InterviewFilterDTO filters, CriteriaBuilder cb, Root<Interview> root) {
        List<Predicate> predicates = new ArrayList<>();
        ZoneId zoneId = ZoneId.systemDefault(); // auto match server timezone
        Expression<LocalDateTime> scheduledTime = root.get("scheduledTime");

        // Apply future filter only if explicitly requested
        if (filters.getTimeRange() == null && filters.getDateFrom() == null && filters.getDateTo() == null) {
            log.debug("No date filters provided - returning all interviews without time restriction");
        }

        if (filters.getMode() != null) {
            predicates.add(cb.equal(root.get("mode"), filters.getMode()));
        }

        if (filters.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filters.getStatus()));
        }

        if (filters.getTimeRange() != null) {
            LocalDateTime now = LocalDateTime.now(zoneId);
            LocalDateTime startDate = now;
            LocalDateTime endDate = now;

            switch (filters.getTimeRange()) {
                case NEXT_3_DAYS:
                    endDate = now.plusDays(3);
                    break;
                case NEXT_WEEK:
                    endDate = now.plusWeeks(1);
                    break;
                case NEXT_MONTH:
                    endDate = now.plusMonths(1);
                    break;
                case CUSTOM:
                    if (filters.getDateFrom() != null) {
                        startDate = filters.getDateFrom().atStartOfDay();
                    }
                    if (filters.getDateTo() != null) {
                        endDate = filters.getDateTo().atTime(LocalTime.MAX);
                    }
                    break;
            }

            predicates.add(cb.between(scheduledTime, startDate, endDate));
        }

        return predicates;
    }

}

