package com.yje.aicodegenerator.utils;

import java.io.IOException;
import java.nio.file.Path;

public final class LocalResourcePaths {
    private LocalResourcePaths() {
    }

    public static Path resolve(Path root, String relative) {
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (!target.startsWith(base) || relative.contains("\\")) {
            throw new IllegalArgumentException("Invalid resource path");
        }
        return target;
    }

    public static Path existingFile(Path root, String relative) throws IOException {
        Path target = resolve(root, relative).toRealPath();
        if (!target.startsWith(root.toRealPath())) {
            throw new IllegalArgumentException("Resource outside storage directory");
        }
        return target;
    }
}
