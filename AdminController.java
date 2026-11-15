package com.internship.controller;

import com.internship.model.*;
import com.internship.repository.*;
import com.internship.service.NotificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository users;
    private final StudentRepository students;
    private final CompanyRepository companies;
    private final InternshipRepository internships;
    private final ApplicationRepository applications;
    private final ScamReportRepository scamReports;
    private final CertificateRepository certificates;
    private final NotificationService notifier;

    public AdminController(UserRepository users, StudentRepository students, CompanyRepository companies,
                           InternshipRepository internships, ApplicationRepository applications,
                           ScamReportRepository scamReports, CertificateRepository certificates,
                           NotificationService notifier) {
        this.scamReports = scamReports;
        this.certificates = certificates;
        this.notifier = notifier;
        this.users = users;
        this.students = students;
        this.companies = companies;
        this.internships = internships;
        this.applications = applications;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            statusCounts.put(s.name(), applications.countByStatus(s));
        }
        model.addAttribute("studentCount", students.count());
        model.addAttribute("companyCount", companies.count());
        model.addAttribute("internshipCount", internships.count());
        model.addAttribute("applicationCount", applications.count());
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("pendingCompanies", companies.findByVerifiedFalse());
        model.addAttribute("pendingInternships", internships.findByApprovedFalse());
        model.addAttribute("openScamReports", scamReports.findByReportedTrueOrderByCreatedAtDesc().stream()
                .filter(r -> "OPEN".equals(r.getAdminStatus())).count());
        return "admin/dashboard";
    }

    // ---------- companies ----------
    @GetMapping("/companies")
    public String companyList(Model model) {
        model.addAttribute("companies", companies.findAll());
        return "admin/companies";
    }

    @PostMapping("/companies/{id}/verify")
    public String verify(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean value,
                         @RequestHeader(value = "Referer", required = false) String referer,
                         RedirectAttributes ra) {
        companies.findById(id).ifPresent(c -> {
            c.setVerified(value);
            companies.save(c);
            ra.addFlashAttribute("success", c.getCompanyName() + (value ? " verified." : " verification revoked."));
        });
        return back(referer, "/admin/companies");
    }

    // ---------- internships ----------
    @GetMapping("/internships")
    public String internshipList(Model model) {
        model.addAttribute("internships", internships.findAll());
        return "admin/internships";
    }

    @PostMapping("/internships/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean value,
                          @RequestHeader(value = "Referer", required = false) String referer,
                          RedirectAttributes ra) {
        internships.findById(id).ifPresent(i -> {
            i.setApproved(value);
            internships.save(i);
            ra.addFlashAttribute("success", i.getTitle() + (value ? " approved." : " hidden from students."));
        });
        return back(referer, "/admin/internships");
    }

    // ---------- users ----------
    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("users", users.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        users.findById(id).ifPresent(u -> {
            if (u.getRole() == Role.ADMIN || u.getEmail().equals(principal.getName())) {
                ra.addFlashAttribute("error", "Admin accounts cannot be disabled here.");
            } else {
                u.setEnabled(!u.isEnabled());
                users.save(u);
                ra.addFlashAttribute("success", u.getName() + (u.isEnabled() ? " enabled." : " disabled."));
            }
        });
        return "redirect:/admin/users";
    }

    // ---------- applications & reports ----------
    @GetMapping("/applications")
    public String applicationList(Model model) {
        model.addAttribute("applications", applications.findAll());
        return "admin/applications";
    }

    @GetMapping("/reports/applications.csv")
    public ResponseEntity<String> csv() {
        StringBuilder sb = new StringBuilder(
                "Application ID,Student,Register Number,Department,Company,Internship,Applied Date,Status\n");
        for (Application a : applications.findAll()) {
            sb.append(a.getId()).append(',')
              .append(q(a.getStudent().getUser().getName())).append(',')
              .append(q(a.getStudent().getRegisterNumber())).append(',')
              .append(q(a.getStudent().getDepartment())).append(',')
              .append(q(a.getInternship().getCompany().getCompanyName())).append(',')
              .append(q(a.getInternship().getTitle())).append(',')
              .append(a.getAppliedDate()).append(',')
              .append(a.getStatus()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications-report.csv")
                .contentType(new MediaType("text", "csv"))
                .body(sb.toString());
    }

    // ---------- scam reports ----------
    @GetMapping("/scam-reports")
    public String scamReportList(Model model) {
        model.addAttribute("reports", scamReports.findByReportedTrueOrderByCreatedAtDesc());
        return "admin/scam-reports";
    }

    @PostMapping("/scam-reports/{id}/review")
    public String reviewScam(@PathVariable Long id, @RequestParam String decision,
                             @RequestParam(required = false) String note, RedirectAttributes ra) {
        scamReports.findById(id).ifPresent(r -> {
            if (!decision.equals("CONFIRMED_SCAM") && !decision.equals("DISMISSED")) return;
            r.setAdminStatus(decision);
            r.setAdminNote(note == null || note.isBlank() ? null : note.trim());
            scamReports.save(r);
            notifier.notify(r.getStudent(), "Admin reviewed your scam report: "
                    + (decision.equals("CONFIRMED_SCAM") ? "confirmed as a scam. Please do not respond to it."
                    : "no scam confirmed, but stay careful."), "/student/scam");
            ra.addFlashAttribute("success", "Report reviewed.");
        });
        return "redirect:/admin/scam-reports";
    }

    // ---------- analytics ----------
    @GetMapping("/analytics")
    public String analytics(Model model) {
        long totalApps = applications.count();
        long selected = applications.countByStatus(ApplicationStatus.SELECTED);
        Map<String, Long> perCompany = new LinkedHashMap<>();
        Map<String, Long> placedPerCompany = new LinkedHashMap<>();
        for (Company c : companies.findAll()) {
            perCompany.put(c.getCompanyName(), (long) internships.findByCompanyOrderByIdDesc(c).size());
            placedPerCompany.put(c.getCompanyName(), applications.findAll().stream()
                    .filter(a -> a.getStatus() == ApplicationStatus.SELECTED
                            && a.getInternship().getCompany().getId().equals(c.getId())).count());
        }
        model.addAttribute("studentCount", students.count());
        model.addAttribute("internshipCount", internships.count());
        model.addAttribute("companyCount", companies.count());
        model.addAttribute("verifiedCompanies", companies.findAll().stream().filter(Company::isVerified).count());
        model.addAttribute("applicationCount", totalApps);
        model.addAttribute("placed", selected);
        model.addAttribute("successRate", totalApps == 0 ? 0 : Math.round(1000.0 * selected / totalApps) / 10.0);
        model.addAttribute("certificateCount", certificates.count());
        model.addAttribute("perCompany", perCompany);
        model.addAttribute("placedPerCompany", placedPerCompany);
        return "admin/analytics";
    }

    private String q(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String back(String referer, String fallback) {
        if (referer != null && referer.contains("/admin/dashboard")) return "redirect:/admin/dashboard";
        return "redirect:" + fallback;
    }
}
