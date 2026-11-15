package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "progress_reports")
@Getter @Setter @NoArgsConstructor
public class ProgressReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "application_id") private Application application;
    private int weekNumber;
    @Column(length = 2000, nullable = false) private String content;
    private LocalDate submittedOn = LocalDate.now();
    @Column(length = 1000) private String mentorFeedback;
}
