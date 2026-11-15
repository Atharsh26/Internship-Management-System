package com.internship.controller;

import com.internship.model.*;
import com.internship.repository.*;
import com.internship.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Student <-> company/mentor messaging, one thread per application. */
@Controller
public class MessageController {
    private final ApplicationRepository applications;
    private final MessageRepository messages;
    private final UserRepository users;
    private final NotificationService notifier;

    public MessageController(ApplicationRepository applications, MessageRepository messages,
                             UserRepository users, NotificationService notifier) {
        this.applications = applications;
        this.messages = messages;
        this.users = users;
        this.notifier = notifier;
    }

    private boolean isParticipant(Application a, String email) {
        return a.getStudent().getUser().getEmail().equals(email)
                || a.getInternship().getCompany().getUser().getEmail().equals(email);
    }

    @GetMapping("/messages/application/{id}")
    public String thread(@PathVariable Long id, Authentication auth, Model model) {
        Application a = applications.findById(id).orElse(null);
        if (a == null || !isParticipant(a, auth.getName())) return "redirect:/";
        model.addAttribute("app", a);
        model.addAttribute("items", messages.findByApplicationOrderBySentAtAsc(a));
        model.addAttribute("me", auth.getName());
        model.addAttribute("isStudent", a.getStudent().getUser().getEmail().equals(auth.getName()));
        return "messages";
    }

    @PostMapping("/messages/application/{id}")
    public String send(@PathVariable Long id, @RequestParam String content, Authentication auth, RedirectAttributes ra) {
        Application a = applications.findById(id).orElse(null);
        if (a == null || !isParticipant(a, auth.getName())) return "redirect:/";
        if (content.isBlank() || content.length() > 2000) {
            ra.addFlashAttribute("error", "Write a message (max 2000 characters).");
            return "redirect:/messages/application/" + id;
        }
        User me = users.findByEmail(auth.getName()).orElseThrow();
        Message m = new Message();
        m.setApplication(a);
        m.setSender(me);
        m.setContent(content.trim());
        messages.save(m);
        boolean fromStudent = a.getStudent().getUser().getId().equals(me.getId());
        User other = fromStudent ? a.getInternship().getCompany().getUser() : a.getStudent().getUser();
        notifier.notify(other, "New message about " + a.getInternship().getTitle() + " from " + me.getName() + ".",
                "/messages/application/" + id);
        return "redirect:/messages/application/" + id;
    }
}
