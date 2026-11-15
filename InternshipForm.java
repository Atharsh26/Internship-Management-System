package com.internship.model;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter @Setter
public class InternshipForm {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 3000, message = "Description is too long (max 3000)")
    private String description;

    @NotBlank(message = "Eligibility is required")
    @Size(max = 1000, message = "Eligibility is too long (max 1000)")
    private String eligibility;

    private String location;
    private String domain;
    @PositiveOrZero(message = "Stipend cannot be negative")
    private Integer stipend;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;
}
