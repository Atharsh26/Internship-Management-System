package com.internship.controller;

import com.internship.model.Role;
import com.internship.model.User;
import com.internship.repository.CompanyRepository;
import com.internship.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/** Profile & settings page for company and admin users (students use /student/profile). */
@Controller
public class AccountController {

    private final UserRepository users;
    private final CompanyRepository companies;

    public AccountController(UserRepository users, CompanyRepository companies) {
        this.users = users;
        this.companies = companies;
    }

    @GetMapping("/account")
    public String account(Principal principal, Model model) {
        User u = users.findByEmail(principal.getName()).orElseThrow();
        if (u.getRole() == Role.STUDENT) return "redirect:/student/profile";
        model.addAttribute("user", u);
        model.addAttribute("company", u.getRole() == Role.COMPANY
                ? companies.findByUserEmail(u.getEmail()).orElse(null) : null);
        return "account";
    }
}
