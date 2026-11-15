package com.internship.service;

import com.internship.model.*;
import com.internship.repository.CertificateRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CertificateService {
    private final CertificateRepository certificates;
    private final NotificationService notifications;

    public CertificateService(CertificateRepository certificates, NotificationService notifications) {
        this.certificates = certificates;
        this.notifications = notifications;
    }

    public Certificate issue(Application a) {
        return certificates.findByApplication(a).orElseGet(() -> {
            Certificate c = new Certificate();
            c.setApplication(a);
            c.setCertificateId("IMS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
            certificates.save(c);
            notifications.notify(a.getStudent().getUser(),
                    "Your internship completion certificate for " + a.getInternship().getTitle() + " is ready.",
                    "/student/certificates");
            return c;
        });
    }
}
