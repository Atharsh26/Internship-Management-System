package com.internship.controller;

import com.internship.repository.NotificationRepository;
import com.internship.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the unread-notification count to every page for logged-in users. */
@ControllerAdvice
public class GlobalModelAdvice {
    private final NotificationRepository notifications;
    private final UserRepository users;

    public GlobalModelAdvice(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @ModelAttribute("unreadCount")
    public long unreadCount(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return 0;
        return users.findByEmail(auth.getName()).map(notifications::countByUserAndReadFalse).orElse(0L);
    }
}
