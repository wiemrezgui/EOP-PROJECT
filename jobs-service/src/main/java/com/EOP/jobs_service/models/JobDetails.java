package com.EOP.jobs_service.models;

import com.EOP.common_lib.common.enums.Department;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class JobDetails implements Serializable {
    @NotBlank
    private String description;
    private Department department;

    // Responsibilities & Requirements
    private List<@NotBlank String> responsibilities;
    private List<@NotBlank String> qualifications;

    // Compensation
    private SalaryRange salary;

    // Skills & Languages
    private List<@NotBlank String> requiredSkills;
    private List<LanguageRequirement> languages;

    // Education & Experience
    private String educationLevel;
    private Integer experienceYearsMin;

    private String applicationInstructions;

    @Getter @Setter
    public static class SalaryRange implements Serializable {
        private BigDecimal min;
        private BigDecimal max;
        private String currency;
    }

    @Getter @Setter
    public static class LanguageRequirement implements Serializable {
        @NotBlank
        private String code;
        private String proficiency; // BASIC, FLUENT, NATIVE
    }
}
