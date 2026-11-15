package com.internship.controller;

import com.internship.model.Application;
import com.internship.repository.ApplicationRepository;
import com.internship.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** Lets the owning company, the applying student, or an admin download a resume. */
@Controller
public class ResumeController {

    private final ApplicationRepository applications;
    private final FileStorageService storage;

    public ResumeController(ApplicationRepository applications, FileStorageService storage) {
        this.applications = applications;
        this.storage = storage;
    }

    @GetMapping("/resumes/application/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        Application a = applications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean admin = hasRole(auth, "ROLE_ADMIN");
        boolean company = hasRole(auth, "ROLE_COMPANY")
                && a.getInternship().getCompany().getUser().getEmail().equals(auth.getName());
        boolean student = hasRole(auth, "ROLE_STUDENT")
                && a.getStudent().getUser().getEmail().equals(auth.getName());
        if (!(admin || company || student)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        String stored = a.getStudent().getResumePath();
        Resource r = stored == null ? null : storage.load(stored);
        if (r == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found");

        String ext = stored.substring(stored.lastIndexOf('.'));
        String downloadName = a.getStudent().getUser().getName().replaceAll("[^A-Za-z0-9]+", "_") + "_resume" + ext;
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(downloadName).build().toString())
                .body(r);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals(role));
    }
}
