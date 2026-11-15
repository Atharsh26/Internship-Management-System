# Internship Management System

Java web application built with **Spring Boot, Thymeleaf, Spring Security, Spring Data JPA (Hibernate) and MySQL**.

## Roles
- **Student** – register, manage profile, upload resume, browse/search internships, apply, track status.
- **Company** – register, post internships with eligibility, view applicants, download resumes, shortlist / select / reject.
- **Admin / Placement coordinator** – verify companies, approve postings, enable/disable users, monitor applications, download CSV report.

## Workflow
Company registers -> Admin verifies -> Company posts internship -> Admin approves -> Student applies -> Company reviews -> Status: Pending -> Shortlisted / Selected / Rejected

## Requirements
- JDK 17+
- Maven 3.8+ (or open in IntelliJ IDEA / VS Code with Java extensions)
- MySQL 8+ running locally
- Lombok plugin in your IDE (IntelliJ has it built in; VS Code's Java extension supports it)

## Setup
1. Make sure MySQL is running. The database `internship_db` is created automatically.
2. Open `src/main/resources/application.properties` and set `spring.datasource.username` and `spring.datasource.password`.
3. Run:
   ```
   mvn spring-boot:run
   ```
   or run `InternshipManagementApplication` from your IDE.
4. Open http://localhost:8080

## UI & exports
- Font: Google Sans (loaded from Google Fonts, falls back to system fonts offline).
- Dark theme is the default. Switch to Light on the **Profile** page (saved in the browser).
- Profile page also has **Download your data (Excel)** buttons: students get their applications; companies get internships/applicants; admins get students, companies, internships, applications, users, or everything in one workbook.

## Default admin login
- Email: `admin@college.edu`
- Password: `admin123`  (change it in `application.properties` before first run)

## Trying it out
1. Register a company at `/register/company`, register a student at `/register/student`.
2. Log in as admin -> verify the company.
3. Log in as the company -> post an internship.
4. Log in as admin -> approve the posting.
5. Log in as the student -> upload resume in Profile -> apply.
6. Log in as the company -> View applications -> change status.

## Structure
```
src/main/java/com/internship/
  controller/   web request handlers (Auth, Home, Student, Company, Admin, Resume)
  service/      registration, file storage, UserDetailsService
  repository/   Spring Data JPA repositories
  model/        entities + form objects
  config/       Spring Security + default admin
src/main/resources/
  templates/    Thymeleaf pages (student/, company/, admin/, fragments/)
  static/       css, js, images
```
Uploaded resumes are stored in the `uploads/` folder next to where the app is run.

## Deferred (post-MVP)
Resume analysis, email notifications, advanced analytics, internship completion tracking.

---
## Added features (from the feature specification)

**Student:** extended profile (education, skills, certifications, GitHub/LinkedIn), search + filters (location, domain, minimum stipend), internship detail page with skill-gap check, 6-stage application tracking (Applied → Under Review → Shortlisted → Interview Scheduled → Selected / Rejected), withdraw & re-apply, upcoming interviews, notifications, tasks + weekly progress reports + mentor feedback, certificates, messaging, **Scam Check** (risk score + report to admin), **Resume Analyzer** and **Skill Gap Analyzer**.

**Company:** shortlist / reject / status update, candidate profile page, resume download, interview scheduling with automatic notification, custom notifications, task assignment, feedback on weekly reports, mark complete + issue certificate, company profile editing, domain and stipend on postings.

**Admin:** scam-report review (confirm/dismiss), analytics page (students, internships, placed, success rate, company participation), existing user/company/internship management.

**Public:** `/verify` lets anyone check a certificate ID.

Notes
* New tables are created automatically (`spring.jpa.hibernate.ddl-auto=update`). Existing data is kept; `PENDING` is displayed as "Applied".
* The resume analyzer reads text-based **PDF and DOCX** files (not scanned images or old `.doc`).
* A new dependency (Apache PDFBox) was added to `pom.xml`: click **Reload Maven** in IntelliJ.
* Not implemented: QR codes on certificates (verification works by certificate ID), extra application documents beyond the resume, meeting scheduling inside messaging (interview scheduling covers it).
