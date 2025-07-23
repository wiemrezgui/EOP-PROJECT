package com.EOP.jobs_service.model;

import com.vladmihalcea.hibernate.type.json.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDate postedDate;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JobDetails details;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    private Set<JobApplication> applications = new HashSet<>();
}

