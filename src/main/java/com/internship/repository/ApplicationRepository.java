package com.internship.repository;

import com.internship.model.Application;
import com.internship.model.ApplicationStatus;
import com.internship.model.Internship;
import com.internship.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudentOrderByAppliedDateDescIdDesc(Student student);
    List<Application> findByInternshipOrderByAppliedDateDescIdDesc(Internship internship);
    boolean existsByStudentAndInternship(Student student, Internship internship);
    long countByStatus(ApplicationStatus status);
    long countByInternship(Internship internship);
}
