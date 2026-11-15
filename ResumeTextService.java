package com.internship.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/** Extracts plain text from a stored PDF or DOCX resume. Legacy .doc files are not supported. */
@Service
public class ResumeTextService {
    private final FileStorageService storage;

    public ResumeTextService(FileStorageService storage) { this.storage = storage; }

    /** Returns the resume text, or null if it cannot be read. */
    public String extract(String storedName) {
        if (storedName == null) return null;
        Resource r = storage.load(storedName);
        if (r == null) return null;
        String lower = storedName.toLowerCase();
        try {
            if (lower.endsWith(".pdf")) {
                try (PDDocument doc = Loader.loadPDF(r.getFile())) {
                    return new PDFTextStripper().getText(doc);
                }
            }
            if (lower.endsWith(".docx")) {
                try (InputStream in = r.getInputStream(); XWPFDocument d = new XWPFDocument(in);
                     XWPFWordExtractor ex = new XWPFWordExtractor(d)) {
                    return ex.getText();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
