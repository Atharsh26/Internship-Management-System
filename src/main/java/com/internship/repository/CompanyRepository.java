package com.internship.repository;

import com.internship.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByUserEmail(String email);
    List<Company> findByVerifiedFalse();
}
