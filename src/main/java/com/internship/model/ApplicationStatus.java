package com.internship.model;

/** PENDING is shown to users as "Applied" (kept as PENDING so existing database rows stay valid). */
public enum ApplicationStatus {
    PENDING("Applied"), UNDER_REVIEW("Under Review"), SHORTLISTED("Shortlisted"),
    INTERVIEW_SCHEDULED("Interview Scheduled"), SELECTED("Selected"), REJECTED("Rejected"),
    WITHDRAWN("Withdrawn");

    private final String label;
    ApplicationStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
    public boolean isClosed() { return this == REJECTED || this == WITHDRAWN; }
}
