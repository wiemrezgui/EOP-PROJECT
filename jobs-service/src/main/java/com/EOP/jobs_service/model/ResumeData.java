package com.EOP.jobs_service.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class ResumeData implements Serializable {
    private PersonalInfo personalInfo;
    private List<Education> education;
    private List<WorkExperience> experience;
    private List<Skill> skills;
    private List<Certification> certifications;

    @Getter @Setter
    public static class PersonalInfo implements Serializable {
        private String fullName;
        private String address;
        private String phone;
        private String linkedInUrl;
        private String portfolioUrl;
    }

    @Getter @Setter
    public static class Education implements Serializable {
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
    }

    @Getter @Setter
    public static class WorkExperience implements Serializable {
        private String company;
        private String position;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean current;
        private List<String> achievements;
    }

    @Getter
    @Setter
    public static class Skill implements Serializable {
        private String name;
        private String level; // BEGINNER, INTERMEDIATE, EXPERT
        private Integer yearsOfExperience;
    }

    @Getter @Setter
    public static class Certification implements Serializable {
        private String name;
        private String issuer;
        private LocalDate dateObtained;
        private LocalDate expiryDate;
        private String credentialId;
    }
}
