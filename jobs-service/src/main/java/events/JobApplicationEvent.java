package events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationEvent {
    private String candidateEmail;
    private Long jobId;
    private String jobTitle;
    private LocalDate applicationDate;
}