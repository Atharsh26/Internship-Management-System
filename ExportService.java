package com.internship.service;

import com.internship.model.*;
import com.internship.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Builds .xlsx workbooks for the "Download your data" buttons. */
@Service
public class ExportService {

    public record ExportFile(String filename, byte[] data) {}

    private final UserRepository users;
    private final StudentRepository students;
    private final CompanyRepository companies;
    private final InternshipRepository internships;
    private final ApplicationRepository applications;

    public ExportService(UserRepository users, StudentRepository students, CompanyRepository companies,
                         InternshipRepository internships, ApplicationRepository applications) {
        this.users = users;
        this.students = students;
        this.companies = companies;
        this.internships = internships;
        this.applications = applications;
    }

    /** Returns null when the export type is unknown or not allowed for this role/user. */
    public ExportFile export(String type, String email, boolean admin, boolean company, boolean student) {
        try (Workbook wb = new XSSFWorkbook()) {
            if (admin) {
                switch (type) {
                    case "students":     studentsSheet(wb); break;
                    case "companies":    companiesSheet(wb); break;
                    case "internships":  internshipsSheet(wb, "Internships", internships.findAll()); break;
                    case "applications": applicationsSheet(wb, "Applications", applications.findAll()); break;
                    case "users":        usersSheet(wb); break;
                    case "all-data":
                        studentsSheet(wb); companiesSheet(wb);
                        internshipsSheet(wb, "Internships", internships.findAll());
                        applicationsSheet(wb, "Applications", applications.findAll());
                        usersSheet(wb);
                        break;
                    default: return null;
                }
            } else if (company) {
                Company c = companies.findByUserEmail(email).orElse(null);
                if (c == null) return null;
                List<Internship> mine = internships.findByCompanyOrderByIdDesc(c);
                List<Application> apps = new ArrayList<>();
                for (Internship i : mine) apps.addAll(applications.findByInternshipOrderByAppliedDateDescIdDesc(i));
                switch (type) {
                    case "my-internships": internshipsSheet(wb, "My Internships", mine); break;
                    case "my-applicants":  applicationsSheet(wb, "Applicants", apps); break;
                    case "my-all":
                        internshipsSheet(wb, "My Internships", mine);
                        applicationsSheet(wb, "Applicants", apps);
                        break;
                    default: return null;
                }
            } else if (student) {
                Student s = students.findByUserEmail(email).orElse(null);
                if (s == null || !type.equals("my-applications")) return null;
                applicationsSheet(wb, "My Applications", applications.findByStudentOrderByAppliedDateDescIdDesc(s));
            } else {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ExportFile(type + ".xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------- sheets ----------
    private void studentsSheet(Workbook wb) {
        List<Object[]> rows = new ArrayList<>();
        for (Student s : students.findAll()) {
            rows.add(new Object[]{s.getId(), s.getUser().getName(), s.getUser().getEmail(), s.getRegisterNumber(),
                    s.getDepartment(), yn(s.getResumePath() != null), yn(s.getUser().isEnabled())});
        }
        sheet(wb, "Students", new String[]{"ID", "Name", "Email", "Register No", "Department", "Resume uploaded", "Account active"}, rows);
    }

    private void companiesSheet(Workbook wb) {
        List<Object[]> rows = new ArrayList<>();
        for (Company c : companies.findAll()) {
            rows.add(new Object[]{c.getId(), c.getCompanyName(), c.getUser().getName(), c.getUser().getEmail(),
                    yn(c.isVerified()), yn(c.getUser().isEnabled())});
        }
        sheet(wb, "Companies", new String[]{"ID", "Company", "Contact", "Email", "Verified", "Account active"}, rows);
    }

    private void internshipsSheet(Workbook wb, String name, List<Internship> list) {
        List<Object[]> rows = new ArrayList<>();
        for (Internship i : list) {
            rows.add(new Object[]{i.getId(), i.getTitle(), i.getCompany().getCompanyName(), i.getLocation(),
                    i.getEligibility(), i.getDeadline(), yn(i.isApproved()), applications.countByInternship(i)});
        }
        sheet(wb, name, new String[]{"ID", "Title", "Company", "Location", "Eligibility", "Deadline", "Approved", "Applicants"}, rows);
    }

    private void applicationsSheet(Workbook wb, String name, List<Application> list) {
        List<Object[]> rows = new ArrayList<>();
        for (Application a : list) {
            Student s = a.getStudent();
            rows.add(new Object[]{a.getId(), s.getUser().getName(), s.getUser().getEmail(), s.getRegisterNumber(),
                    s.getDepartment(), a.getInternship().getCompany().getCompanyName(), a.getInternship().getTitle(),
                    a.getAppliedDate(), a.getStatus()});
        }
        sheet(wb, name, new String[]{"ID", "Student", "Email", "Register No", "Department", "Company", "Internship", "Applied on", "Status"}, rows);
    }

    private void usersSheet(Workbook wb) {
        List<Object[]> rows = new ArrayList<>();
        for (User u : users.findAll()) {
            rows.add(new Object[]{u.getId(), u.getName(), u.getEmail(), u.getRole(), yn(u.isEnabled())});
        }
        sheet(wb, "Users", new String[]{"ID", "Name", "Email", "Role", "Active"}, rows);
    }

    // ---------- helpers ----------
    private String yn(boolean b) {
        return b ? "Yes" : "No";
    }

    private void sheet(Workbook wb, String name, String[] headers, List<Object[]> rows) {
        Sheet sh = wb.createSheet(name);
        CellStyle head = wb.createCellStyle();
        Font bold = wb.createFont();
        bold.setBold(true);
        head.setFont(bold);
        head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        head.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row hr = sh.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            Cell cell = hr.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(head);
            sh.setColumnWidth(c, 22 * 256);
        }
        int r = 1;
        for (Object[] row : rows) {
            Row xr = sh.createRow(r++);
            for (int c = 0; c < row.length; c++) {
                Object v = row[c];
                Cell cell = xr.createCell(c);
                if (v == null) cell.setCellValue("");
                else if (v instanceof Number n) cell.setCellValue(n.doubleValue());
                else cell.setCellValue(v.toString());
            }
        }
        sh.createFreezePane(0, 1);
    }
}
