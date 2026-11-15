package com.internship.repository;

import com.internship.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByApplicationOrderBySentAtAsc(Application a);
    void deleteByApplication(Application a);
}
