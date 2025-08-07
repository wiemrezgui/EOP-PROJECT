package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.DTOs.JobFilterDTO;
import com.EOP.jobs_service.interfaces.JobRepositoryCustom;
import com.EOP.jobs_service.models.Job;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<Job> findWithFilters(JobFilterDTO filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Job> query = cb.createQuery(Job.class);
        Root<Job> root = query.from(Job.class);

        List<Predicate> predicates = buildPredicates(filters, cb, root);
        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<Job> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Job> result = typedQuery.getResultList();
        return new PageImpl<>(result, pageable, countWithFilters(filters));
    }

    @Override
    public long countWithFilters(JobFilterDTO filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Job> root = query.from(Job.class);

        List<Predicate> predicates = buildPredicates(filters, cb, root);
        query.select(cb.count(root));
        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    private List<Predicate> buildPredicates(JobFilterDTO filters, CriteriaBuilder cb, Root<Job> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            predicates.add(root.get("status").in(filters.getStatuses()));
        }

        if (filters.getPostedDate() != null) {
            LocalDate fromDate = calculateFromDate(filters.getPostedDate());
            predicates.add(cb.greaterThanOrEqualTo(root.get("postedDate"), fromDate));

            if (filters.getPostedDate().getRange() == JobFilterDTO.DateRangeOption.CUSTOM
                    && filters.getPostedDate().getCustomTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("postedDate"),
                        filters.getPostedDate().getCustomTo()));
            }
        }

        if (filters.getDepartments() != null && !filters.getDepartments().isEmpty()) {
            predicates.add(cb.function("jsonb_extract_path_text", String.class,
                            root.get("details"), cb.literal("department"))
                    .in(filters.getDepartments().stream().map(Enum::name).collect(Collectors.toList())));
        }

        if (filters.getSalaryRange() != null) {
            // Handle salary filtering without explicit cast
            if (filters.getSalaryRange().getMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.function("cast_text_to_numeric", BigDecimal.class,
                                cb.function("jsonb_extract_path_text", String.class,
                                        root.get("details"), cb.literal("salary"), cb.literal("min"))),
                        filters.getSalaryRange().getMin()
                ));
            }
            if (filters.getSalaryRange().getMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        cb.function("cast_text_to_numeric", BigDecimal.class,
                                cb.function("jsonb_extract_path_text", String.class,
                                        root.get("details"), cb.literal("salary"), cb.literal("max"))),
                        filters.getSalaryRange().getMax()
                ));
            }
            if (filters.getSalaryRange().getCurrency() != null) {
                predicates.add(cb.equal(
                        cb.function("jsonb_extract_path_text", String.class,
                                root.get("details"), cb.literal("salary"), cb.literal("currency")),
                        filters.getSalaryRange().getCurrency()
                ));
            }
        }

        if (filters.getEducationLevel() != null) {
            predicates.add(cb.equal(
                    cb.function("jsonb_extract_path_text", String.class,
                            root.get("details"), cb.literal("educationLevel")),
                    filters.getEducationLevel()
            ));
        }

        if (filters.getMinExperienceYears() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    cb.function("cast_text_to_integer", Integer.class,
                            cb.function("jsonb_extract_path_text", String.class,
                                    root.get("details"), cb.literal("experienceYearsMin"))),
                    filters.getMinExperienceYears()
            ));
        }

        if (filters.getRequiredSkills() != null && !filters.getRequiredSkills().isEmpty()) {
            // Use jsonb_exists_any for better PostgreSQL array handling
            for (String skill : filters.getRequiredSkills()) {
                predicates.add(cb.isTrue(
                        cb.function("jsonb_exists", Boolean.class,
                                cb.function("jsonb_extract_path", String.class,
                                        root.get("details"), cb.literal("requiredSkills")),
                                cb.literal(skill)
                        )
                ));
            }
        }

        return predicates;
    }

    private LocalDate calculateFromDate(JobFilterDTO.PostedDateRange postedDate) {
        if (postedDate.getRange() == JobFilterDTO.DateRangeOption.CUSTOM) {
            return postedDate.getCustomFrom() != null ? postedDate.getCustomFrom() : LocalDate.MIN;
        }

        return switch (postedDate.getRange()) {
            case LAST_WEEK -> LocalDate.now().minusWeeks(1);
            case LAST_MONTH -> LocalDate.now().minusMonths(1);
            case LAST_3_MONTHS -> LocalDate.now().minusMonths(3);
            case LAST_6_MONTHS -> LocalDate.now().minusMonths(6);
            case LAST_YEAR -> LocalDate.now().minusYears(1);
            default -> LocalDate.MIN;
        };
    }
}