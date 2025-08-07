package com.EOP.jobs_service.repositories;

import com.EOP.jobs_service.DTOs.CandidateFilterDTO;
import com.EOP.jobs_service.interfaces.CandidateRepositoryCustom;
import com.EOP.jobs_service.models.Candidate;
import com.EOP.jobs_service.models.JobApplication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CandidateRepositoryImpl implements CandidateRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public Page<Candidate> findWithFilters(CandidateFilterDTO filters, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Candidate> query = cb.createQuery(Candidate.class);
        Root<Candidate> root = query.from(Candidate.class);

        List<Predicate> predicates = buildPredicates(filters, cb, root);
        query.where(predicates.toArray(new Predicate[0]));

        TypedQuery<Candidate> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Candidate> result = typedQuery.getResultList();
        return new PageImpl<>(result, pageable, countWithFilters(filters));
    }

    @Override
    public long countWithFilters(CandidateFilterDTO filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Candidate> root = query.from(Candidate.class);

        List<Predicate> predicates = buildPredicates(filters, cb, root);
        query.select(cb.count(root));
        query.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(query).getSingleResult();
    }

    private List<Predicate> buildPredicates(CandidateFilterDTO filters, CriteriaBuilder cb, Root<Candidate> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
            predicates.add(root.get("status").in(filters.getStatuses()));
        }

        if (filters.getAppliedFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("appliedDate"), filters.getAppliedFrom()));
        }

        if (filters.getAppliedTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("appliedDate"), filters.getAppliedTo()));
        }

        if (filters.getJobId() != null) {
            Join<Candidate, JobApplication> applications = root.join("applications");
            predicates.add(cb.equal(applications.get("job").get("id"), filters.getJobId()));
        }

        return predicates;
    }
}
