package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "internship_tasks")
@Getter @Setter @NoArgsConstructor
public class InternshipTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "application_id") private Application application;
    @Column(nullable = false) private String title;
    @Column(length = 1000) private String description;
    private LocalDate dueDate;
    private boolean done = false;
}
