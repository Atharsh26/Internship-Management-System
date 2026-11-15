package com.internship.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.*;

@Entity @Table(name = "messages")
@Getter @Setter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "application_id") private Application application;
    @ManyToOne(optional = false) @JoinColumn(name = "sender_id") private User sender;
    @Column(length = 2000, nullable = false) private String content;
    private LocalDateTime sentAt = LocalDateTime.now();
}
