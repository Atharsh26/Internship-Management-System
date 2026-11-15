package com.internship.controller;

import com.internship.model.Certificate;
import com.internship.repository.CertificateRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Printable certificate (owner/issuer/admin) and PUBLIC verification page for employers. */
@Controller
public class CertificateController {
    private final CertificateRepository certificates;

    public CertificateController(CertificateRepository certificates) {
        this.certificates = certificates;
    }

    @GetMapping("/certificates/{certId}")
    public String view(@PathVariable String certId, Authentication auth, Model model) {
        Certificate c = certificates.findByCertificateId(certId).orElse(null);
        if (c == null) return "redirect:/";
        boolean admin = auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
        boolean student = c.getApplication().getStudent().getUser().getEmail().equals(auth.getName());
        boolean company = c.getApplication().getInternship().getCompany().getUser().getEmail().equals(auth.getName());
        if (!(admin || student || company)) return "redirect:/";
        model.addAttribute("c", c);
        return "certificate";
    }

    @GetMapping("/verify")
    public String verifyForm() {
        return "verify";
    }

    @GetMapping("/verify/{certId}")
    public String verify(@PathVariable String certId, Model model) {
        model.addAttribute("id", certId);
        model.addAttribute("c", certificates.findByCertificateId(certId.trim().toUpperCase()).orElse(null));
        return "verify";
    }

    @PostMapping("/verify")
    public String verifyPost(@RequestParam String certId) {
        return "redirect:/verify/" + org.springframework.web.util.UriUtils.encodePathSegment(certId.trim(),
                java.nio.charset.StandardCharsets.UTF_8);
    }
}
