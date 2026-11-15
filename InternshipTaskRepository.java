package com.internship.repository;

import com.internship.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface InternshipTaskRepository extends JpaRepository<InternshipTask, Long> {
    List<InternshipTask> findByApplicationOrderByIdAsc(Application a);
    void deleteByApplication(Application a);
}
