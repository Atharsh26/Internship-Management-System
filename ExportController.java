package com.internship.controller;

import com.internship.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ExportController {

    private final ExportService exports;

    public ExportController(ExportService exports) {
        this.exports = exports;
    }

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(@PathVariable String type, Authentication auth) {
        ExportService.ExportFile f = exports.export(type, auth.getName(),
                has(auth, "ROLE_ADMIN"), has(auth, "ROLE_COMPANY"), has(auth, "ROLE_STUDENT"));
        if (f == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown export");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + f.filename())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(f.data());
    }

    private boolean has(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals(role));
    }
}
