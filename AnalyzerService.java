package com.internship.service;

import com.internship.model.Internship;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/** Shared engine for the AI Resume Analyzer and the Skill Gap Analyzer (keyword based). */
@Service
public class AnalyzerService {

    public record Analysis(int matchPercent, List<String> matched, List<String> missing,
                           List<String> tips, Map<String, String> resources) {}

    /** skill -> aliases that count as a mention. */
    private static final Map<String, String[]> SKILLS = new LinkedHashMap<>();
    static {
        add("Java"); add("Python"); add("C++", "c\\+\\+"); add("C#", "c#", "c sharp"); add("JavaScript", "javascript", "js");
        add("TypeScript"); add("SQL"); add("MySQL"); add("PostgreSQL", "postgres", "postgresql"); add("MongoDB");
        add("HTML"); add("CSS"); add("React", "react", "reactjs", "react.js"); add("Angular"); add("Node.js", "node", "nodejs", "node.js");
        add("Spring Boot", "spring boot", "springboot", "spring"); add("Hibernate"); add("REST API", "restful", "rest api", "rest apis");
        add("Git"); add("GitHub"); add("Docker"); add("Kubernetes"); add("AWS"); add("Azure"); add("Linux");
        add("Machine Learning", "machine learning", "ml"); add("Deep Learning"); add("Data Analysis", "data analysis", "data analytics");
        add("Pandas"); add("NumPy"); add("TensorFlow"); add("Excel"); add("Power BI", "power bi", "powerbi"); add("Tableau");
        add("Android"); add("Flutter"); add("Selenium"); add("Testing", "testing", "junit", "qa");
        add("Data Structures", "data structures", "dsa", "algorithms"); add("OOP", "oop", "object oriented", "object-oriented");
        add("Communication"); add("Teamwork"); add("Problem Solving", "problem solving", "problem-solving");
        add("Figma"); add("UI/UX", "ui/ux", "ux", "ui design"); add("Cyber Security", "cyber security", "cybersecurity", "security");
    }
    private static void add(String name, String... aliases) {
        SKILLS.put(name, aliases.length == 0 ? new String[]{name.toLowerCase()} : aliases);
    }

    public Set<String> detectSkills(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null) return found;
        String t = text.toLowerCase();
        for (Map.Entry<String, String[]> e : SKILLS.entrySet()) {
            for (String alias : e.getValue()) {
                String a = alias.toLowerCase();
                // aliases containing regex escapes are already escaped; others are quoted
                String rx = a.contains("\\") ? a : Pattern.quote(a);
                if (Pattern.compile("(?<![a-z0-9+#])" + rx + "(?![a-z0-9+#])").matcher(t).find()) {
                    found.add(e.getKey());
                    break;
                }
            }
        }
        return found;
    }

    public String requirementsText(Internship i) {
        return i.getTitle() + " " + i.getDescription() + " " + (i.getEligibility() == null ? "" : i.getEligibility())
                + " " + (i.getDomain() == null ? "" : i.getDomain());
    }

    /** @param candidateText resume text or the skills typed into the profile. */
    public Analysis compare(String candidateText, Internship internship, boolean resumeMode) {
        Set<String> have = detectSkills(candidateText);
        Set<String> need = detectSkills(requirementsText(internship));
        List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
        for (String n : need) (have.contains(n) ? matched : missing).add(n);
        int pct = need.isEmpty() ? 0 : (int) Math.round(100.0 * matched.size() / need.size());

        List<String> tips = new ArrayList<>();
        if (need.isEmpty()) tips.add("This posting does not name specific known skills, so a percentage match could not be calculated.");
        if (!missing.isEmpty()) tips.add("Learn or add evidence of: " + String.join(", ", missing) + ".");
        if (resumeMode) {
            String t = candidateText == null ? "" : candidateText.toLowerCase();
            if (candidateText != null && candidateText.length() < 600) tips.add("Your resume is very short - add projects, coursework and achievements.");
            if (!t.contains("project")) tips.add("Add a Projects section describing what you built and the tools you used.");
            if (!t.contains("github") && !t.contains("linkedin")) tips.add("Add your GitHub and LinkedIn links.");
            if (!t.matches("(?s).*\\d+%.*|(?s).*\\d+\\+? (users|students|projects|applications).*")) tips.add("Use numbers to show impact (e.g. 'reduced load time by 30%').");
            if (!t.contains("education")) tips.add("Add a clear Education section with your degree, college and CGPA.");
            if (!t.contains("skill")) tips.add("Add a dedicated Skills section so recruiters can scan it quickly.");
            if (!t.contains("intern") && !t.contains("experience")) tips.add("Mention any internships, hackathons or volunteering experience.");
            if (!matched.isEmpty()) tips.add("Move the matching skills (" + String.join(", ", matched) + ") near the top of your resume.");
        }
        Map<String, String> res = new LinkedHashMap<>();
        for (String m : missing) {
            String q = java.net.URLEncoder.encode(m, java.nio.charset.StandardCharsets.UTF_8);
            res.put(m, "https://www.youtube.com/results?search_query=" + q + "+full+course+for+beginners");
        }
        return new Analysis(pct, matched, missing, tips, res);
    }
}
