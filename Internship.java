package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "internships")
@Getter @Setter @NoArgsConstructor
public class Internship {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000, nullable = false)
    private String description;

    @Column(length = 1000)
    private String eligibility;

    private String location;
    private String domain;
    /** Monthly stipend in rupees; null or 0 = unpaid. */
    private Integer stipend;
    private LocalDate deadline;

    /** Set to true by the admin to make the posting visible to students. */
    private boolean approved = false;
}
