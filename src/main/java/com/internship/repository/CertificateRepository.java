package com.internship.repository;

import com.internship.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByCertificateId(String id);
    Optional<Certificate> findByApplication(Application a);
    void deleteByApplication(Application a);
}
