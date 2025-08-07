package com.EOP.jobs_service.DTOs;

import com.EOP.common_lib.common.enums.Department;
import com.EOP.jobs_service.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class JobFilterDTO {
    private List<JobStatus> statuses;
    private PostedDateRange postedDate;
    private List<Department> departments;
    private SalaryRangeFilter salaryRange;
    private String educationLevel;
    private Integer minExperienceYears;
    private List<String> requiredSkills;

    @Getter
    @Setter
    public static class PostedDateRange {
        private DateRangeOption range;
        private LocalDate customFrom;
        private LocalDate customTo;
    }

    @Getter
    @Setter
    public static class SalaryRangeFilter {
        private BigDecimal min;
        private BigDecimal max;
        private String currency;
    }

    public enum DateRangeOption {
        LAST_WEEK,
        LAST_MONTH,
        LAST_3_MONTHS,
        LAST_6_MONTHS,
        LAST_YEAR,
        CUSTOM
    }
}
