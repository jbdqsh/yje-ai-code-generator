package com.yje.aicodegenerator.service.impl;

import com.yje.aicodegenerator.service.FileStorageService;
import com.yje.aicodegenerator.utils.LocalResourcePaths;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class LocalFileStorageService implements FileStorageService {
    @Value("${storage.local.root:${user.dir}/uploads}")
    private Path root;

    @Value("${storage.local.public-base-url:http://localhost:8123/api/files}")
    private String publicBaseUrl;

    @PostConstruct
    public void initialize() throws IOException {
        Files.createDirectories(root);
    }

    public Path root() {
        return root;
    }

    @Override
    public String save(String key, File source) {
        String relative = key.replaceFirst("^/+", "");
        if (relative.isBlank()) {
            throw new IllegalArgumentException("File key must not be empty");
        }
        Path target = LocalResourcePaths.resolve(root, relative);
        try {
            Files.createDirectories(target.getParent());
            if (!target.getParent().toRealPath().startsWith(root.toRealPath()) || Files.isSymbolicLink(target)) {
                throw new IllegalArgumentException("Invalid storage destination");
            }
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return publicBaseUrl.replaceAll("/+$", "") + "/"
                    + UriUtils.encodePath(relative, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException("Cannot save local file", error);
        }
    }
}
