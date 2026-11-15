package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(length = 500, nullable = false) private String message;
    private String link;
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "is_read") private boolean read = false;
}
