package com.internship.controller;

import com.internship.model.Notification;
import com.internship.model.User;
import com.internship.repository.NotificationRepository;
import com.internship.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/** Notifications for every role. */
@Controller
public class NotificationController {
    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationController(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @GetMapping("/notifications")
    public String list(Principal principal, Model model) {
        User u = users.findByEmail(principal.getName()).orElseThrow();
        model.addAttribute("items", notifications.findByUserOrderByCreatedAtDesc(u));
        return "notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(Principal principal) {
        User u = users.findByEmail(principal.getName()).orElseThrow();
        List<Notification> list = notifications.findByUserOrderByCreatedAtDesc(u);
        list.forEach(n -> n.setRead(true));
        notifications.saveAll(list);
        return "redirect:/notifications";
    }

    /** Marks one notification read and goes to the page it points to. */
    @GetMapping("/notifications/{id}/open")
    public String open(@PathVariable Long id, Principal principal) {
        Notification n = notifications.findById(id).orElse(null);
        if (n == null || !n.getUser().getEmail().equals(principal.getName())) return "redirect:/notifications";
        n.setRead(true);
        notifications.save(n);
        String link = n.getLink();
        return (link != null && link.startsWith("/") && !link.startsWith("//")) ? "redirect:" + link : "redirect:/notifications";
    }
}
