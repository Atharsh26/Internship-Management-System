package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "interviews")
@Getter @Setter @NoArgsConstructor
public class Interview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "application_id") private Application application;
    private LocalDateTime scheduledAt;
    private String mode;          // Online / On-site
    private String locationOrLink;
    @Column(length = 500) private String notes;
}
