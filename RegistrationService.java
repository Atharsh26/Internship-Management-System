package com.internship.service;

import com.internship.model.*;
import com.internship.repository.CompanyRepository;
import com.internship.repository.StudentRepository;
import com.internship.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final StudentRepository students;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, StudentRepository students,
                               CompanyRepository companies, PasswordEncoder encoder) {
        this.users = users;
        this.students = students;
        this.companies = companies;
        this.encoder = encoder;
    }

    public boolean emailTaken(String email) {
        return users.existsByEmail(email.trim().toLowerCase());
    }

    @Transactional
    public void registerStudent(RegisterForm f) {
        User u = createUser(f, Role.STUDENT);
        Student s = new Student();
        s.setUser(u);
        s.setRegisterNumber(f.getRegisterNumber().trim());
        s.setDepartment(f.getDepartment() == null ? null : f.getDepartment().trim());
        students.save(s);
    }

    @Transactional
    public void registerCompany(RegisterForm f) {
        User u = createUser(f, Role.COMPANY);
        Company c = new Company();
        c.setUser(u);
        c.setCompanyName(f.getCompanyName().trim());
        c.setVerified(false);
        companies.save(c);
    }

    private User createUser(RegisterForm f, Role role) {
        User u = new User();
        u.setName(f.getName().trim());
        u.setEmail(f.getEmail().trim().toLowerCase());
        u.setPassword(encoder.encode(f.getPassword()));
        u.setRole(role);
        return users.save(u);
    }
}
