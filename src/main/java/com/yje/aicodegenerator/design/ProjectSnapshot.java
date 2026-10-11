package com.yje.aicodegenerator.design;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class ProjectSnapshot {
    public void backup(Path project, Path snapshot) throws IOException {
        copy(project, snapshot);
    }

    public void restore(Path snapshot, Path project) throws IOException {
        Files.walkFileTree(project, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return excluded(dir, project) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                // Keep directories that contain excluded dependency trees.
                try (var children = Files.list(dir)) {
                    if (!dir.equals(project) && children.findAny().isEmpty()) Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        copy(snapshot, project);
    }

    private void copy(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (excluded(dir, source)) return FileVisitResult.SKIP_SUBTREE;
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink()) throw new IOException("Snapshots cannot include symbolic links");
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean excluded(Path dir, Path root) {
        return !dir.equals(root) && (dir.getFileName().toString().equals("node_modules")
                || dir.getFileName().toString().equals(".git"));
    }
}
