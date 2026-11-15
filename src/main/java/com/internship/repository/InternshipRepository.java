package com.internship.repository;

import com.internship.model.Company;
import com.internship.model.Internship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InternshipRepository extends JpaRepository<Internship, Long> {
    List<Internship> findByApprovedTrueAndDeadlineGreaterThanEqualOrderByDeadlineAsc(LocalDate today);
    List<Internship> findByCompanyOrderByIdDesc(Company company);
    List<Internship> findByApprovedFalse();
}
