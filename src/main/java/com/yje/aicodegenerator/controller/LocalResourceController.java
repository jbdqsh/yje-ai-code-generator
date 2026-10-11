package com.yje.aicodegenerator.controller;

import com.yje.aicodegenerator.constant.AppConstant;
import com.yje.aicodegenerator.service.impl.LocalFileStorageService;
import com.yje.aicodegenerator.utils.LocalResourcePaths;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class LocalResourceController {
    private final LocalFileStorageService storage;
    private Path deployRoot = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR);

    public LocalResourceController(LocalFileStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/deploy/{key}/**")
    public ResponseEntity<Resource> deployed(@PathVariable String key, HttpServletRequest request) {
        if (!key.matches("[A-Za-z0-9_-]+")) {
            return ResponseEntity.notFound().build();
        }
        String path = mappedPath(request).substring(("/deploy/" + key).length());
        if (path.isEmpty()) {
            return ResponseEntity.status(302).header("Location", request.getRequestURI() + "/").build();
        }
        Path root = deployRoot.resolve(key);
        String relative = path.substring(1);
        return serve(root, relative.isEmpty() ? "index.html" : relative);
    }

    @GetMapping("/files/**")
    public ResponseEntity<Resource> uploaded(HttpServletRequest request) {
        return serve(storage.root(), mappedPath(request).substring("/files/".length()));
    }

    private String mappedPath(HttpServletRequest request) {
        return (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
    }

    private ResponseEntity<Resource> serve(Path root, String relative) {
        try {
            Path file = LocalResourcePaths.existingFile(root, relative);
            if (!Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok().contentType(mediaType)
                    .header("X-Content-Type-Options", "nosniff").body(resource);
        } catch (IOException | IllegalArgumentException error) {
            return ResponseEntity.notFound().build();
        }
    }
}
