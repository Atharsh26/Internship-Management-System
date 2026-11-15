package com.internship.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth == null) return "redirect:/login";
        for (GrantedAuthority g : auth.getAuthorities()) {
            switch (g.getAuthority()) {
                case "ROLE_ADMIN":   return "redirect:/admin/dashboard";
                case "ROLE_COMPANY": return "redirect:/company/dashboard";
                case "ROLE_STUDENT": return "redirect:/student/dashboard";
                default: break;
            }
        }
        return "redirect:/login";
    }
}
