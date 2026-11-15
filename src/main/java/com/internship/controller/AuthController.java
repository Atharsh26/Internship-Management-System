package com.internship.controller;

import com.internship.model.RegisterForm;
import com.internship.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final RegistrationService registration;

    public AuthController(RegistrationService registration) {
        this.registration = registration;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register/{type}")
    public String showRegister(@PathVariable String type, Model model) {
        if (!isValidType(type)) return "redirect:/login";
        model.addAttribute("type", type);
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register/{type}")
    public String register(@PathVariable String type,
                           @Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult result, Model model) {
        if (!isValidType(type)) return "redirect:/login";
        model.addAttribute("type", type);

        boolean student = type.equals("student");
        if (student && isBlank(form.getRegisterNumber())) {
            result.rejectValue("registerNumber", "required", "Register number is required");
        }
        if (!student && isBlank(form.getCompanyName())) {
            result.rejectValue("companyName", "required", "Company name is required");
        }
        if (!result.hasFieldErrors("email") && registration.emailTaken(form.getEmail())) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
        }
        if (result.hasErrors()) return "register";

        if (student) registration.registerStudent(form);
        else registration.registerCompany(form);
        return "redirect:/login?registered";
    }

    private boolean isValidType(String t) {
        return t.equals("student") || t.equals("company");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
