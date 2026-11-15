package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "scam_reports")
@Getter @Setter @NoArgsConstructor
public class ScamReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id") private User student;
    private String senderEmail;
    private String subject;
    @Column(length = 4000) private String content;
    private int riskScore;
    private String riskLevel;     // LOW / MEDIUM / HIGH
    @Column(length = 2000) private String reasons;
    /** true once the student pressed "Report to admin". */
    private boolean reported = false;
    private String adminStatus = "OPEN";   // OPEN / CONFIRMED_SCAM / DISMISSED
    @Column(length = 500) private String adminNote;
    private LocalDateTime createdAt = LocalDateTime.now();
}
