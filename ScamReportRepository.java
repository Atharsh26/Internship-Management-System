package com.internship.repository;

import com.internship.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ScamReportRepository extends JpaRepository<ScamReport, Long> {
    List<ScamReport> findByStudentOrderByCreatedAtDesc(User u);
    List<ScamReport> findByReportedTrueOrderByCreatedAtDesc();
}
