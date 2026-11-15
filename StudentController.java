package com.internship.controller;

import com.internship.model.*;
import com.internship.repository.*;
import com.internship.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentRepository students;
    private final UserRepository users;
    private final InternshipRepository internships;
    private final ApplicationRepository applications;
    private final FileStorageService storage;
    private final InterviewRepository interviews;
    private final NotificationRepository notifications;
    private final NotificationService notifier;
    private final InternshipTaskRepository tasks;
    private final ProgressReportRepository reports;
    private final ScamReportRepository scamReports;
    private final ScamDetectionService scamService;
    private final CertificateRepository certificates;
    private final AnalyzerService analyzer;
    private final ResumeTextService resumeText;

    public StudentController(StudentRepository students, UserRepository users, InternshipRepository internships,
                             ApplicationRepository applications, FileStorageService storage,
                             InterviewRepository interviews, NotificationRepository notifications,
                             NotificationService notifier, InternshipTaskRepository tasks,
                             ProgressReportRepository reports, ScamReportRepository scamReports,
                             ScamDetectionService scamService, CertificateRepository certificates,
                             AnalyzerService analyzer, ResumeTextService resumeText) {
        this.students = students; this.users = users; this.internships = internships;
        this.applications = applications; this.storage = storage; this.interviews = interviews;
        this.notifications = notifications; this.notifier = notifier; this.tasks = tasks;
        this.reports = reports; this.scamReports = scamReports; this.scamService = scamService;
        this.certificates = certificates; this.analyzer = analyzer; this.resumeText = resumeText;
    }

    private Student current(Principal p) {
        return students.findByUserEmail(p.getName()).orElseThrow();
    }

    /** Returns the application only if it belongs to this student. */
    private Application ownApp(Long id, Student s) {
        Application a = applications.findById(id).orElse(null);
        return (a != null && a.getStudent().getId().equals(s.getId())) ? a : null;
    }

    // ---------------------------------------------------------------- dashboard
    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Student s = current(principal);
        List<Application> apps = applications.findByStudentOrderByAppliedDateDescIdDesc(s);
        Map<String, Long> counts = apps.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
        Map<Long, Boolean> hasCert = new HashMap<>();
        for (Application a : apps) hasCert.put(a.getId(), certificates.findByApplication(a).isPresent());
        List<Notification> latest = notifications.findByUserOrderByCreatedAtDesc(s.getUser());
        model.addAttribute("student", s);
        model.addAttribute("applications", apps);
        model.addAttribute("counts", counts);
        model.addAttribute("hasCert", hasCert);
        model.addAttribute("upcomingInterviews",
                interviews.findByApplicationStudentAndScheduledAtAfterOrderByScheduledAtAsc(s, LocalDateTime.now()));
        model.addAttribute("latestNotifications", latest.size() > 5 ? latest.subList(0, 5) : latest);
        model.addAttribute("openCount",
                internships.findByApprovedTrueAndDeadlineGreaterThanEqualOrderByDeadlineAsc(LocalDate.now()).size());
        return "student/dashboard";
    }

    // ---------------------------------------------------------------- profile
    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("student", current(principal));
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String registerNumber,
                                @RequestParam(required = false) String department,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String education,
                                @RequestParam(required = false) String skills,
                                @RequestParam(required = false) String certifications,
                                @RequestParam(required = false) String githubUrl,
                                @RequestParam(required = false) String linkedinUrl,
                                @RequestParam(required = false) MultipartFile resume,
                                Principal principal, RedirectAttributes ra) {
        Student s = current(principal);
        if (name.isBlank() || registerNumber.isBlank()) {
            ra.addFlashAttribute("error", "Name and register number are required.");
            return "redirect:/student/profile";
        }
        if (!validLink(githubUrl) || !validLink(linkedinUrl)) {
            ra.addFlashAttribute("error", "GitHub and LinkedIn links must start with http:// or https://");
            return "redirect:/student/profile";
        }
        try {
            if (resume != null && !resume.isEmpty()) {
                s.setResumePath(storage.saveResume(resume));
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/profile";
        }
        s.getUser().setName(name.trim());
        users.save(s.getUser());
        s.setRegisterNumber(registerNumber.trim());
        s.setDepartment(clean(department));
        s.setPhone(clean(phone));
        s.setEducation(clean(education));
        s.setSkills(clean(skills));
        s.setCertifications(clean(certifications));
        s.setGithubUrl(clean(githubUrl));
        s.setLinkedinUrl(clean(linkedinUrl));
        students.save(s);
        ra.addFlashAttribute("success", "Profile updated.");
        return "redirect:/student/profile";
    }

    private String clean(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }

    private boolean validLink(String v) {
        return v == null || v.isBlank() || v.trim().toLowerCase().startsWith("http://")
                || v.trim().toLowerCase().startsWith("https://");
    }

    // ---------------------------------------------------------------- search & apply
    @GetMapping("/internships")
    public String browse(@RequestParam(required = false) String q,
                         @RequestParam(required = false) String location,
                         @RequestParam(required = false) String domain,
                         @RequestParam(required = false) Integer minStipend,
                         Principal principal, Model model) {
        Student s = current(principal);
        List<Internship> all = internships
                .findByApprovedTrueAndDeadlineGreaterThanEqualOrderByDeadlineAsc(LocalDate.now());
        List<Internship> list = all;
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            list = list.stream().filter(i ->
                    i.getTitle().toLowerCase().contains(needle)
                    || i.getCompany().getCompanyName().toLowerCase().contains(needle)
                    || (i.getEligibility() != null && i.getEligibility().toLowerCase().contains(needle))
                    || (i.getDomain() != null && i.getDomain().toLowerCase().contains(needle))
                    || (i.getLocation() != null && i.getLocation().toLowerCase().contains(needle)))
                    .collect(Collectors.toList());
        }
        if (location != null && !location.isBlank()) {
            list = list.stream().filter(i -> i.getLocation() != null
                    && i.getLocation().equalsIgnoreCase(location.trim())).collect(Collectors.toList());
        }
        if (domain != null && !domain.isBlank()) {
            list = list.stream().filter(i -> i.getDomain() != null
                    && i.getDomain().equalsIgnoreCase(domain.trim())).collect(Collectors.toList());
        }
        if (minStipend != null && minStipend > 0) {
            list = list.stream().filter(i -> i.getStipend() != null && i.getStipend() >= minStipend)
                    .collect(Collectors.toList());
        }
        Set<Long> appliedIds = applications.findByStudentOrderByAppliedDateDescIdDesc(s).stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN)
                .map(a -> a.getInternship().getId()).collect(Collectors.toSet());
        model.addAttribute("internships", list);
        model.addAttribute("appliedIds", appliedIds);
        model.addAttribute("locations", all.stream().map(Internship::getLocation)
                .filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().sorted().collect(Collectors.toList()));
        model.addAttribute("domains", all.stream().map(Internship::getDomain)
                .filter(x -> x != null && !x.isBlank()).map(String::trim).distinct().sorted().collect(Collectors.toList()));
        model.addAttribute("q", q);
        model.addAttribute("location", location);
        model.addAttribute("domain", domain);
        model.addAttribute("minStipend", minStipend);
        return "student/internships";
    }

    @GetMapping("/internships/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Student s = current(principal);
        Internship i = internships.findById(id).orElse(null);
        if (i == null || !i.isApproved()) {
            ra.addFlashAttribute("error", "Internship not found.");
            return "redirect:/student/internships";
        }
        Application existing = applications.findByStudentOrderByAppliedDateDescIdDesc(s).stream()
                .filter(a -> a.getInternship().getId().equals(id) && a.getStatus() != ApplicationStatus.WITHDRAWN)
                .findFirst().orElse(null);
        model.addAttribute("i", i);
        model.addAttribute("existing", existing);
        model.addAttribute("open", !i.getDeadline().isBefore(LocalDate.now()));
        model.addAttribute("gap", analyzer.compare(s.getSkills(), i, false));
        return "student/internship-detail";
    }

    @PostMapping("/internships/{id}/apply")
    public String apply(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Student s = current(principal);
        Internship i = internships.findById(id).orElse(null);
        if (i == null || !i.isApproved() || i.getDeadline().isBefore(LocalDate.now())) {
            ra.addFlashAttribute("error", "This internship is not open for applications.");
            return "redirect:/student/internships";
        }
        if (s.getResumePath() == null) {
            ra.addFlashAttribute("error", "Please upload your resume before applying.");
            return "redirect:/student/profile";
        }
        Application withdrawn = applications.findByStudentOrderByAppliedDateDescIdDesc(s).stream()
                .filter(a -> a.getInternship().getId().equals(id)).findFirst().orElse(null);
        if (withdrawn != null && withdrawn.getStatus() != ApplicationStatus.WITHDRAWN) {
            ra.addFlashAttribute("error", "You have already applied to this internship.");
            return "redirect:/student/internships";
        }
        Application a = withdrawn != null ? withdrawn : new Application();   // re-apply after withdrawing
        a.setStudent(s);
        a.setInternship(i);
        a.setStatus(ApplicationStatus.PENDING);
        a.setAppliedDate(LocalDate.now());
        applications.save(a);
        notifier.notify(i.getCompany().getUser(),
                s.getUser().getName() + " applied for " + i.getTitle() + ".",
                "/company/internships/" + i.getId() + "/applications");
        ra.addFlashAttribute("success", "Application submitted for " + i.getTitle() + ".");
        return "redirect:/student/dashboard";
    }

    @PostMapping("/applications/{id}/withdraw")
    public String withdraw(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Student s = current(principal);
        Application a = ownApp(id, s);
        if (a == null) {
            ra.addFlashAttribute("error", "Application not found.");
        } else if (a.getStatus() == ApplicationStatus.SELECTED || a.getStatus().isClosed()) {
            ra.addFlashAttribute("error", "This application can no longer be withdrawn.");
        } else {
            a.setStatus(ApplicationStatus.WITHDRAWN);
            applications.save(a);
            notifier.notify(a.getInternship().getCompany().getUser(),
                    s.getUser().getName() + " withdrew the application for " + a.getInternship().getTitle() + ".",
                    "/company/internships/" + a.getInternship().getId() + "/applications");
            ra.addFlashAttribute("success", "Application withdrawn.");
        }
        return "redirect:/student/dashboard";
    }

    // ---------------------------------------------------------------- progress tracking
    @GetMapping("/applications/{id}/progress")
    public String progress(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        Student s = current(principal);
        Application a = ownApp(id, s);
        if (a == null || a.getStatus() != ApplicationStatus.SELECTED) {
            ra.addFlashAttribute("error", "Progress tracking is available once you are selected.");
            return "redirect:/student/dashboard";
        }
        List<InternshipTask> ts = tasks.findByApplicationOrderByIdAsc(a);
        long done = ts.stream().filter(InternshipTask::isDone).count();
        List<ProgressReport> rs = reports.findByApplicationOrderByWeekNumberAsc(a);
        model.addAttribute("app", a);
        model.addAttribute("tasks", ts);
        model.addAttribute("reports", rs);
        model.addAttribute("completion", ts.isEmpty() ? 0 : (int) (100 * done / ts.size()));
        model.addAttribute("nextWeek", rs.isEmpty() ? 1 : rs.get(rs.size() - 1).getWeekNumber() + 1);
        model.addAttribute("certificate", certificates.findByApplication(a).orElse(null));
        return "student/progress";
    }

    @PostMapping("/tasks/{id}/toggle")
    public String toggleTask(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Student s = current(principal);
        InternshipTask t = tasks.findById(id).orElse(null);
        if (t == null || ownApp(t.getApplication().getId(), s) == null) {
            ra.addFlashAttribute("error", "Task not found.");
            return "redirect:/student/dashboard";
        }
        t.setDone(!t.isDone());
        tasks.save(t);
        return "redirect:/student/applications/" + t.getApplication().getId() + "/progress";
    }

    @PostMapping("/applications/{id}/reports")
    public String submitReport(@PathVariable Long id, @RequestParam int weekNumber, @RequestParam String content,
                               Principal principal, RedirectAttributes ra) {
        Student s = current(principal);
        Application a = ownApp(id, s);
        if (a == null || a.getStatus() != ApplicationStatus.SELECTED) {
            ra.addFlashAttribute("error", "You cannot submit reports for this application.");
            return "redirect:/student/dashboard";
        }
        if (content.isBlank() || content.length() > 2000 || weekNumber < 1) {
            ra.addFlashAttribute("error", "Enter a week number and a report (max 2000 characters).");
            return "redirect:/student/applications/" + id + "/progress";
        }
        ProgressReport r = new ProgressReport();
        r.setApplication(a);
        r.setWeekNumber(weekNumber);
        r.setContent(content.trim());
        reports.save(r);
        notifier.notify(a.getInternship().getCompany().getUser(),
                s.getUser().getName() + " submitted the week " + weekNumber + " report.",
                "/company/applications/" + id + "/progress");
        ra.addFlashAttribute("success", "Week " + weekNumber + " report submitted.");
        return "redirect:/student/applications/" + id + "/progress";
    }

    @GetMapping("/certificates")
    public String myCertificates(Principal principal, Model model) {
        Student s = current(principal);
        List<Certificate> list = new ArrayList<>();
        for (Application a : applications.findByStudentOrderByAppliedDateDescIdDesc(s))
            certificates.findByApplication(a).ifPresent(list::add);
        model.addAttribute("certificates", list);
        return "student/certificates";
    }

    // ---------------------------------------------------------------- scam detection
    @GetMapping("/scam")
    public String scamPage(Principal principal, Model model) {
        model.addAttribute("history", scamReports.findByStudentOrderByCreatedAtDesc(current(principal).getUser()));
        return "student/scam";
    }

    @PostMapping("/scam/check")
    public String scamCheck(@RequestParam(required = false) String senderEmail,
                            @RequestParam(required = false) String subject,
                            @RequestParam String content,
                            Principal principal, RedirectAttributes ra) {
        if (content.isBlank() || content.length() > 4000) {
            ra.addFlashAttribute("error", "Paste the email text (max 4000 characters).");
            return "redirect:/student/scam";
        }
        ScamDetectionService.Result r = scamService.analyze(senderEmail, subject, content);
        ScamReport sr = new ScamReport();
        sr.setStudent(current(principal).getUser());
        sr.setSenderEmail(clean(senderEmail));
        sr.setSubject(clean(subject));
        sr.setContent(content.trim());
        sr.setRiskScore(r.score());
        sr.setRiskLevel(r.level());
        String reasons = String.join("\n", r.reasons());
        sr.setReasons(reasons.length() > 2000 ? reasons.substring(0, 2000) : reasons);
        scamReports.save(sr);
        return "redirect:/student/scam/" + sr.getId();
    }

    @GetMapping("/scam/{id}")
    public String scamResult(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        ScamReport sr = scamReports.findById(id).orElse(null);
        if (sr == null || !sr.getStudent().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Result not found.");
            return "redirect:/student/scam";
        }
        model.addAttribute("sr", sr);
        model.addAttribute("reasons", Arrays.asList(sr.getReasons().split("\n")));
        return "student/scam-result";
    }

    @PostMapping("/scam/{id}/report")
    public String scamReport(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        ScamReport sr = scamReports.findById(id).orElse(null);
        if (sr == null || !sr.getStudent().getEmail().equals(principal.getName())) {
            ra.addFlashAttribute("error", "Result not found.");
            return "redirect:/student/scam";
        }
        sr.setReported(true);
        scamReports.save(sr);
        users.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).forEach(u ->
                notifier.notify(u, "A student reported a suspicious internship email.", "/admin/scam-reports"));
        ra.addFlashAttribute("success", "Thank you. The admin will review your report.");
        return "redirect:/student/scam/" + id;
    }

    // ---------------------------------------------------------------- resume analyzer & skill gap
    @GetMapping("/analyzer")
    public String analyzerPage(Principal principal, Model model) {
        loadAnalyzerForm(model, current(principal));
        return "student/analyzer";
    }

    @PostMapping("/analyzer")
    public String analyze(@RequestParam Long internshipId, @RequestParam String mode,
                          Principal principal, Model model) {
        Student s = current(principal);
        loadAnalyzerForm(model, s);
        Internship i = internships.findById(internshipId).orElse(null);
        if (i == null || !i.isApproved()) {
            model.addAttribute("error", "Choose an internship.");
            return "student/analyzer";
        }
        boolean resumeMode = mode.equals("resume");
        String text;
        if (resumeMode) {
            text = resumeText.extract(s.getResumePath());
            if (text == null || text.isBlank()) {
                model.addAttribute("error", s.getResumePath() == null
                        ? "Upload a resume on your profile first."
                        : "Could not read your resume text. Use a text-based PDF or DOCX file (not a scanned image or old .doc).");
                return "student/analyzer";
            }
        } else {
            text = s.getSkills();
            if (text == null || text.isBlank()) {
                model.addAttribute("error", "Add your skills on your profile first.");
                return "student/analyzer";
            }
        }
        model.addAttribute("result", analyzer.compare(text, i, resumeMode));
        model.addAttribute("target", i);
        model.addAttribute("resumeMode", resumeMode);
        model.addAttribute("selected", internshipId);
        return "student/analyzer";
    }

    private void loadAnalyzerForm(Model model, Student s) {
        model.addAttribute("openInternships",
                internships.findByApprovedTrueAndDeadlineGreaterThanEqualOrderByDeadlineAsc(LocalDate.now()));
        model.addAttribute("student", s);
    }
}
