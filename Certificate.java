package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "certificates")
@Getter @Setter @NoArgsConstructor
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String certificateId;
    @OneToOne(optional = false) @JoinColumn(name = "application_id", unique = true) private Application application;
    private LocalDate issuedOn = LocalDate.now();
}
