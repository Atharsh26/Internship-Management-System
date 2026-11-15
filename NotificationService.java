package com.internship.service;

import com.internship.model.Notification;
import com.internship.model.User;
import com.internship.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public void notify(User user, String message, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        n.setLink(link);
        repo.save(n);
    }
}
