package com.yje.aicodegenerator.design;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ProjectSnapshotTest {
    @TempDir Path temp;
    @Test void restoresSourceAndDistWithoutCopyingDependencies() throws Exception {
        Path project = temp.resolve("project"), backup = temp.resolve("backup");
        Files.createDirectories(project.resolve("node_modules"));
        Files.createDirectories(project.resolve("dist"));
        Files.writeString(project.resolve("node_modules/keep"), "dependency");
        Files.writeString(project.resolve("index.html"), "source");
        Files.writeString(project.resolve("dist/index.html"), "built");
        var snapshots = new ProjectSnapshot();
        snapshots.backup(project, backup);
        assertFalse(Files.exists(backup.resolve("node_modules")));
        Files.writeString(project.resolve("index.html"), "broken");
        Files.writeString(project.resolve("extra.html"), "new");
        snapshots.restore(backup, project);
        assertEquals("source", Files.readString(project.resolve("index.html")));
        assertEquals("built", Files.readString(project.resolve("dist/index.html")));
        assertTrue(Files.exists(project.resolve("node_modules/keep")));
        assertFalse(Files.exists(project.resolve("extra.html")));
    }
}
