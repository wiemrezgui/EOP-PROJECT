package com.EOP.jobs_service.DTOs;

import com.EOP.jobs_service.models.JobDetails;
import com.vladmihalcea.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Type;

@Data @NoArgsConstructor @AllArgsConstructor @Getter
@Setter
public class JobDTO {

    @NotBlank(message = "Title is required")
    @Schema(description = "job title", example = "Senior full stack developer")
    private String title;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @NotBlank(message = "Job details are required")
    private JobDetails details;
}
