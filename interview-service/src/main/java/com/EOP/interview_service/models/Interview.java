package com.EOP.interview_service.models;

import com.EOP.interview_service.enums.InterviewMode;
import com.EOP.interview_service.enums.InterviewStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "interview")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RestController
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate scheduledDate;

    @JsonFormat(pattern = "HH-mm-ss")
    private LocalTime scheduledTime;

    private String feedback;
    private String description;
    private String meetingLink;
    private String meetingTitle;
    private String location;
    @Enumerated(EnumType.STRING)
    private InterviewMode mode;
    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    private Integer durationMinutes = 15;
    @Email
    private String userEmail;
    private Long candidateID;
    private Long JobID;

}
