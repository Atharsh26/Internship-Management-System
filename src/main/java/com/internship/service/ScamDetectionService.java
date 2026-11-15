package com.internship.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

/**
 * Rule-based risk scoring for suspicious internship emails.
 * It gives a RISK ASSESSMENT only - it can never prove an email is safe or fraudulent.
 */
@Service
public class ScamDetectionService {

    public record Result(int score, String level, List<String> reasons) {}

    private static final Set<String> FREE_MAIL = Set.of("gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
            "rediffmail.com", "proton.me", "protonmail.com", "aol.com", "mail.com");
    private static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly",
            "rb.gy", "is.gd", "shorturl.at", "ow.ly");
    private static final Set<String> RISKY_TLDS = Set.of("xyz", "top", "click", "work", "loan", "icu", "buzz", "tk", "ml", "cf", "gq");
    private static final String[] PAYMENT = {"registration fee", "security deposit", "processing fee", "pay a fee",
            "training fee", "refundable deposit", "kit fee", "send money", "pay rs", "pay inr", "pay $", "upi", "western union",
            "gift card", "bitcoin", "crypto", "bank account number", "wire transfer", "payment of"};
    private static final String[] URGENCY = {"urgent", "immediately", "act now", "last chance", "within 24 hours",
            "today only", "limited seats", "hurry", "offer expires", "respond asap"};
    private static final String[] TOO_GOOD = {"guaranteed job", "100% placement", "no interview", "no experience required",
            "earn up to", "work from home and earn", "selected without", "high stipend", "easy money", "per day earn"};
    private static final String[] SENSITIVE = {"aadhaar", "aadhar", "pan card", "passport", "otp", "password", "cvv",
            "card number", "bank details", "ssn", "social security"};
    private static final String[] MESSAGING = {"whatsapp", "telegram", "signal group"};
    private static final Pattern URL = Pattern.compile("(?i)\\b((?:https?://|www\\.)[^\\s<>\"')]+)");
    private static final Pattern EMAIL = Pattern.compile("(?i)^[^@\\s]+@([^@\\s]+\\.[a-z]{2,})$");
    private static final Pattern IP_HOST = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    public Result analyze(String senderEmail, String subject, String body) {
        int score = 0;
        List<String> why = new ArrayList<>();
        String text = ((subject == null ? "" : subject) + " " + (body == null ? "" : body)).toLowerCase();

        // ---- sender ----
        String sender = senderEmail == null ? "" : senderEmail.trim();
        if (!sender.isEmpty()) {
            Matcher m = EMAIL.matcher(sender);
            if (!m.matches()) {
                score += 10; why.add("The sender address does not look like a valid email.");
            } else {
                String domain = m.group(1).toLowerCase();
                String tld = domain.substring(domain.lastIndexOf('.') + 1);
                if (FREE_MAIL.contains(domain)) {
                    score += 15; why.add("Sent from a free email provider (" + domain + ") instead of a company domain.");
                }
                if (RISKY_TLDS.contains(tld)) {
                    score += 15; why.add("The sender domain ends in '." + tld + "', which is often used in spam.");
                }
                if (domain.chars().filter(Character::isDigit).count() >= 3) {
                    score += 10; why.add("The sender domain contains many digits.");
                }
                if (domain.split("\\.").length > 3 || domain.contains("-hr") || domain.contains("careers-") || domain.contains("-jobs")) {
                    score += 10; why.add("The sender domain looks like an imitation of a company/HR domain.");
                }
                String local = sender.substring(0, sender.indexOf('@')).toLowerCase();
                if (local.matches(".*(hr|recruit|career|jobs).*\\d{3,}.*")) {
                    score += 5; why.add("The sender name contains random numbers.");
                }
            }
        } else {
            score += 5; why.add("No sender address was given, so the sender could not be checked.");
        }

        // ---- links ----
        Matcher um = URL.matcher(body == null ? "" : body);
        int links = 0;
        while (um.find()) {
            links++;
            String host = hostOf(um.group(1));
            String tld = host.contains(".") ? host.substring(host.lastIndexOf('.') + 1) : "";
            if (SHORTENERS.contains(host)) { score += 15; why.add("Contains a shortened link (" + host + ") that hides its real destination."); }
            else if (IP_HOST.matcher(host).matches()) { score += 20; why.add("Contains a link that uses a raw IP address."); }
            else if (RISKY_TLDS.contains(tld)) { score += 10; why.add("Contains a link to a '." + tld + "' website."); }
            if (um.group(1).toLowerCase().startsWith("http://")) { score += 5; why.add("Contains an unencrypted (http) link."); }
            if (host.contains("@") || host.chars().filter(c -> c == '-').count() >= 3) { score += 5; why.add("A link has an unusual-looking address."); }
        }
        if (links > 3) { score += 5; why.add("Contains many links."); }

        // ---- content ----
        score += hit(text, PAYMENT, 30, 40, "Asks for money or payment details (a very common internship scam sign).", why);
        score += hit(text, SENSITIVE, 20, 25, "Asks for sensitive personal or financial information.", why);
        score += hit(text, TOO_GOOD, 15, 20, "Makes promises that sound too good to be true.", why);
        score += hit(text, URGENCY, 8, 12, "Uses pressure or urgency to make you act quickly.", why);
        score += hit(text, MESSAGING, 8, 8, "Moves the conversation to WhatsApp/Telegram instead of official email.", why);
        if (text.contains("congratulations") && text.contains("selected") && !text.contains("interview")) {
            score += 10; why.add("Says you were selected without any interview or application process.");
        }

        score = Math.min(100, score);
        String level = score >= 60 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
        if (why.isEmpty()) why.add("No common scam patterns were found. This is not a guarantee - always verify the company independently.");
        return new Result(score, level, why);
    }

    private int hit(String text, String[] words, int first, int max, String reason, List<String> why) {
        int found = 0;
        for (String w : words) if (text.contains(w)) found++;
        if (found == 0) return 0;
        why.add(reason);
        return Math.min(max, first + (found - 1) * 5);
    }

    private String hostOf(String url) {
        String u = url.replaceFirst("(?i)^https?://", "").replaceFirst("(?i)^www\\.", "");
        int end = u.length();
        for (char c : new char[]{'/', '?', '#', ':'}) { int i = u.indexOf(c); if (i >= 0 && i < end) end = i; }
        return u.substring(0, end).toLowerCase();
    }
}
