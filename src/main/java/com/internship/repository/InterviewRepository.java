package com.internship.repository;

import com.internship.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplicationOrderByScheduledAtAsc(Application a);
    List<Interview> findByApplicationStudentAndScheduledAtAfterOrderByScheduledAtAsc(Student s, java.time.LocalDateTime t);
    void deleteByApplication(Application a);
}
