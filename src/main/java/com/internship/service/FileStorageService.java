package com.internship.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("pdf", "doc", "docx");
    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Saves a resume and returns the stored file name. */
    public String saveResume(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null || !ALLOWED.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("Resume must be a PDF, DOC or DOCX file.");
        }
        String stored = UUID.randomUUID() + "." + ext.toLowerCase();
        try {
            Files.copy(file.getInputStream(), root.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return stored;
    }

    public Resource load(String storedName) {
        Path p = root.resolve(storedName).normalize();
        if (!p.startsWith(root) || !Files.exists(p)) {
            return null;
        }
        return new FileSystemResource(p);
    }
}
