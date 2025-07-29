package com.EOP.jobs_service.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "candidates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private LocalDate appliedDate;

    @Enumerated(EnumType.STRING)
    private CandidateStatus status;

    @Lob
    @Column(name = "resume")
    private byte[] resume;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL)
    private Set<JobApplication> applications = new HashSet<>();
}
