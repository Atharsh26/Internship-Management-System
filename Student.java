package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter @Setter @NoArgsConstructor
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String registerNumber;
    private String department;
    private String resumePath;
    private String phone;
    @Column(length = 1000) private String education;
    @Column(length = 1000) private String skills;          // comma separated
    @Column(length = 1000) private String certifications;
    private String githubUrl;
    private String linkedinUrl;
}
