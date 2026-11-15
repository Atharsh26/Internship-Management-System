package com.internship.controller;

import com.internship.model.*;
import com.internship.repository.*;
import com.internship.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/company")
public class CompanyController {

    private final CompanyRepository companies;
    private final InternshipRepository internships;
    private final ApplicationRepository applications;
    private final InterviewRepository interviews;
    private final InternshipTaskRepository tasks;
    private final ProgressReportRepository reports;
    private final MessageRepository messages;
    private final CertificateRepository certificates;
    private final CertificateService certificateService;
    private final NotificationService notifier;

    public CompanyController(CompanyRepository companies, InternshipRepository internships,
                             ApplicationRepository applications, InterviewRepository interviews,
                             InternshipTaskRepository tasks, ProgressReportRepository reports,
                             MessageRepository messages, CertificateRepository certificates,
                             CertificateService certificateService, NotificationService notifier) {
        this.companies = companies;
        this.internships = internships;
        this.applications = applications;
        this.interviews = interviews;
        this.tasks = tasks;
        this.reports = reports;
        this.messages = messages;
        this.certificates = certificates;
        this.certificateService = certificateService;
        this.notifier = notifier;
    }

    private Company current(Principal p) {
        return companies.findByUserEmail(p.getName()).orElseThrow();
    }

    private Internship owned(Long id, Company c) {
        Internship i = internships.findById(id).orElse(null);
        return (i != null && i.getCompany().getId().equals(c.getId())) ? i : null;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Company c = current(principal);
        List<Internship> list = internships.findByCompanyOrderByIdDesc(c);
        Map<Long, Long> counts = new HashMap<>();
        for (Internship i : list) counts.put(i.getId(), applications.countByInternship(i));
        model.addAttribute("company", c);
        model.addAttribute("internships", list);
        model.addAttribute("counts", counts);
        model.addAttribute("today", LocalDate.now());
        return "company/dashboard";
    }

    @GetMapping("/internships/new")
    public String newForm(Principal principal, Model model, RedirectAttributes ra) {
        if (!current(principal).isVerified()) {
            ra.addFlashAttribute("error", "Your company must be verified by the admin before posting.");
            return "redirect:/company/dashboard";
        }
        model.addAttribute("form", new InternshipForm());
        return "company/internship-form";
    }

    @PostMapping("/internships")
    public String create(@Valid @ModelAttribute("form") InternshipForm form, BindingResult result,
                         Principal principal, RedirectAttributes ra) {
        Company c = current(principal);
        if (!c.isVerified()) {
            ra.addFlashAttribute("error", "Your company must be verified by the admin before posting.");
            return "redirect:/company/dashboard";
        }
        if (result.hasErrors()) return "company/internship-form";
        Internship i = new Internship();
        i.setCompany(c);
        i.setTitle(form.getTitle().trim());
        i.setDescription(form.getDescription().trim());
        i.setEligibility(form.getEligibility().trim());
        i.setLocation(form.getLocation());
        i.setDomain(form.getDomain() == null || form.getDomain().isBlank() ? null : form.getDomain().trim());
        i.setStipend(form.getStipend());
        i.setDeadline(form.getDeadline());
        i.setApproved(false);
        internships.save(i);
        ra.addFlashAttribute("success", "Internship submitted. It will be visible to students once the admin approves it.");
        return "redirect:/company/dashboard";
    }

    @PostMapping("/internships/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Internship i = owned(id, current(principal));
        if (i == null) {
            ra.addFlashAttribute("error", "Internship not found.");
        } else {
            for (Application a : applications.findByInternshipOrderByAppliedDateDescIdDesc(i)) {
                interviews.deleteByApplication(a);
                tasks.deleteByApplication(a);
                reports.deleteByApplication(a);
                messages.deleteByApplication(a);
                certificates.deleteByApplication(a);
                applications.delete(a);
            }
            internships.delete(i);
            ra.addFlashAttribute("success", "Internship deleted.");
        }
        return "redirect:/company/dashboard";
    }

    @GetMapping("/internships/{id}/applications")
    public String viewApplications(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Internship i = owned(id, current(principal));
        if (i == null) {
            ra.addFlashAttribute("error", "Internship not found.");
            return "redirect:/company/dashboard";
        }
        model.addAttribute("internship", i);
        model.addAttribute("applications", applications.findByInternshipOrderByAppliedDateDescIdDesc(i));
        model.addAttribute("statuses", java.util.Arrays.stream(ApplicationStatus.values())
                .filter(x -> x != ApplicationStatus.WITHDRAWN).toList());
        return "company/applications";
    }

    @PostMapping("/applications/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam ApplicationStatus status,
                               Principal principal, RedirectAttributes ra) {
        Application a = applications.findById(id).orElse(null);
        Company c = current(principal);
        if (a == null || !a.getInternship().getCompany().getId().equals(c.getId())) {
            ra.addFlashAttribute("error", "Application not found.");
            return "redirect:/company/dashboard";
        }
        if (a.getStatus() == ApplicationStatus.WITHDRAWN) {
            ra.addFlashAttribute("error", "The student withdrew this application.");
            return "redirect:/company/internships/" + a.getInternship().getId() + "/applications";
        }
        a.setStatus(status);
        applications.save(a);
        notifier.notify(a.getStudent().getUser(),
                "Your application for " + a.getInternship().getTitle() + " at " + c.getCompanyName()
                        + " is now: " + status.getLabel() + ".", "/student/dashboard");
        ra.addFlashAttribute("success", "Status updated to " + status.getLabel() + ".");
        return "redirect:/company/internships/" + a.getInternship().getId() + "/applications";
    }

    // ---------------------------------------------------------------- candidate management
    private Application ownedApp(Long id, Company c) {
        Application a = applications.findById(id).orElse(null);
        return (a != null && a.getInternship().getCompany().getId().equals(c.getId())) ? a : null;
    }

    @GetMapping("/applications/{id}/candidate")
    public String candidate(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Application a = ownedApp(id, current(principal));
        if (a == null) {
            ra.addFlashAttribute("error", "Application not found.");
            return "redirect:/company/dashboard";
        }
        model.addAttribute("app", a);
        model.addAttribute("student", a.getStudent());
        model.addAttribute("interviews", interviews.findByApplicationOrderByScheduledAtAsc(a));
        return "company/candidate";
    }

    @PostMapping("/applications/{id}/interview")
    public String scheduleInterview(@PathVariable Long id,
                                    @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime scheduledAt,
                                    @RequestParam String mode, @RequestParam(required = false) String locationOrLink,
                                    @RequestParam(required = false) String notes,
                                    Principal principal, RedirectAttributes ra) {
        Company c = current(principal);
        Application a = ownedApp(id, c);
        if (a == null || a.getStatus().isClosed()) {
            ra.addFlashAttribute("error", "Cannot schedule an interview for this application.");
            return "redirect:/company/dashboard";
        }
        if (scheduledAt.isBefore(java.time.LocalDateTime.now())) {
            ra.addFlashAttribute("error", "Interview time must be in the future.");
            return "redirect:/company/internships/" + a.getInternship().getId() + "/applications";
        }
        Interview iv = new Interview();
        iv.setApplication(a);
        iv.setScheduledAt(scheduledAt);
        iv.setMode(mode);
        iv.setLocationOrLink(locationOrLink == null || locationOrLink.isBlank() ? null : locationOrLink.trim());
        iv.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        interviews.save(iv);
        a.setStatus(ApplicationStatus.INTERVIEW_SCHEDULED);
        applications.save(a);
        notifier.notify(a.getStudent().getUser(), "Interview scheduled for " + a.getInternship().getTitle() + " at "
                + c.getCompanyName() + " on " + scheduledAt.toString().replace('T', ' ') + " (" + mode + ").", "/student/dashboard");
        ra.addFlashAttribute("success", "Interview scheduled and the student was notified.");
        return "redirect:/company/internships/" + a.getInternship().getId() + "/applications";
    }

    @PostMapping("/applications/{id}/notify")
    public String sendNotification(@PathVariable Long id, @RequestParam String message,
                                   Principal principal, RedirectAttributes ra) {
        Company c = current(principal);
        Application a = ownedApp(id, c);
        if (a == null || message.isBlank()) {
            ra.addFlashAttribute("error", "Enter a message.");
            return "redirect:/company/dashboard";
        }
        notifier.notify(a.getStudent().getUser(), c.getCompanyName() + ": " + message.trim(), "/student/dashboard");
        ra.addFlashAttribute("success", "Notification sent.");
        return "redirect:/company/internships/" + a.getInternship().getId() + "/applications";
    }

    // ---------------------------------------------------------------- tasks, progress, certificate
    @GetMapping("/applications/{id}/progress")
    public String progress(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Application a = ownedApp(id, current(principal));
        if (a == null || a.getStatus() != ApplicationStatus.SELECTED) {
            ra.addFlashAttribute("error", "Progress is available for selected interns only.");
            return "redirect:/company/dashboard";
        }
        List<InternshipTask> ts = tasks.findByApplicationOrderByIdAsc(a);
        long done = ts.stream().filter(InternshipTask::isDone).count();
        model.addAttribute("app", a);
        model.addAttribute("tasks", ts);
        model.addAttribute("reports", reports.findByApplicationOrderByWeekNumberAsc(a));
        model.addAttribute("completion", ts.isEmpty() ? 0 : (int) (100 * done / ts.size()));
        model.addAttribute("certificate", certificates.findByApplication(a).orElse(null));
        return "company/progress";
    }

    @PostMapping("/applications/{id}/tasks")
    public String addTask(@PathVariable Long id, @RequestParam String title,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dueDate,
                          Principal principal, RedirectAttributes ra) {
        Application a = ownedApp(id, current(principal));
        if (a == null || a.getStatus() != ApplicationStatus.SELECTED || title.isBlank()) {
            ra.addFlashAttribute("error", "Cannot add this task.");
            return "redirect:/company/dashboard";
        }
        InternshipTask t = new InternshipTask();
        t.setApplication(a);
        t.setTitle(title.trim());
        t.setDescription(description == null || description.isBlank() ? null : description.trim());
        t.setDueDate(dueDate);
        tasks.save(t);
        notifier.notify(a.getStudent().getUser(), "New task assigned: " + t.getTitle(),
                "/student/applications/" + id + "/progress");
        ra.addFlashAttribute("success", "Task assigned.");
        return "redirect:/company/applications/" + id + "/progress";
    }

    @PostMapping("/reports/{id}/feedback")
    public String feedback(@PathVariable Long id, @RequestParam String feedback,
                           Principal principal, RedirectAttributes ra) {
        ProgressReport r = reports.findById(id).orElse(null);
        Company c = current(principal);
        if (r == null || ownedApp(r.getApplication().getId(), c) == null || feedback.length() > 1000) {
            ra.addFlashAttribute("error", "Cannot save feedback.");
            return "redirect:/company/dashboard";
        }
        r.setMentorFeedback(feedback.isBlank() ? null : feedback.trim());
        reports.save(r);
        notifier.notify(r.getApplication().getStudent().getUser(),
                "Mentor feedback added to your week " + r.getWeekNumber() + " report.",
                "/student/applications/" + r.getApplication().getId() + "/progress");
        ra.addFlashAttribute("success", "Feedback saved.");
        return "redirect:/company/applications/" + r.getApplication().getId() + "/progress";
    }

    @PostMapping("/applications/{id}/certificate")
    public String issueCertificate(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Application a = ownedApp(id, current(principal));
        if (a == null || a.getStatus() != ApplicationStatus.SELECTED) {
            ra.addFlashAttribute("error", "Certificates are only issued to selected interns.");
            return "redirect:/company/dashboard";
        }
        Certificate cert = certificateService.issue(a);
        ra.addFlashAttribute("success", "Certificate " + cert.getCertificateId() + " issued.");
        return "redirect:/company/applications/" + id + "/progress";
    }

    // ---------------------------------------------------------------- company profile
    @PostMapping("/profile")
    public String updateProfile(@RequestParam String companyName,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) String website,
                                Principal principal, RedirectAttributes ra) {
        Company c = current(principal);
        if (companyName.isBlank()) {
            ra.addFlashAttribute("error", "Company name is required.");
            return "redirect:/account";
        }
        if (website != null && !website.isBlank() && !website.trim().toLowerCase().matches("https?://.+")) {
            ra.addFlashAttribute("error", "Website must start with http:// or https://");
            return "redirect:/account";
        }
        c.setCompanyName(companyName.trim());
        c.setDescription(description == null || description.isBlank() ? null : description.trim());
        c.setWebsite(website == null || website.isBlank() ? null : website.trim());
        companies.save(c);
        ra.addFlashAttribute("success", "Company profile updated.");
        return "redirect:/account";
    }
}
