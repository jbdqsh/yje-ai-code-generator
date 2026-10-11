package com.yje.aicodegenerator.utils;

import cn.hutool.core.io.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ChromeScreenshotCapture {
    private ChromeScreenshotCapture() {
    }

    static void capture(String url, Path output) throws IOException, InterruptedException {
        capture(url, output, 1600, 900);
    }

    public static void capture(String url, Path output, int width, int height) throws IOException, InterruptedException {
        Path profile = Files.createTempDirectory("yje-screenshot-chrome-");
        Process process = null;
        try {
            process = new ProcessBuilder(findChrome(), "--headless=new", "--disable-gpu",
                    "--no-sandbox", "--hide-scrollbars", "--no-first-run", "--disable-extensions",
                    "--window-size=" + width + "," + height, "--remote-debugging-port=0",
                    "--user-data-dir=" + profile.toAbsolutePath(),
                    "about:blank")
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            try {
                ChromeViewportCapture.capture(profile, url, output, width, height);
            } catch (InterruptedException error) {
                throw error;
            } catch (Exception error) {
                throw new IOException("Chrome screenshot failed", error);
            }
            if (!Files.isRegularFile(output) || Files.size(output) == 0) {
                throw new IOException("Chrome did not produce a screenshot");
            }
        } finally {
            if (process != null && process.isAlive()) {
                var children = process.descendants().toList();
                children.forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                for (var child : children) {
                    try { child.onExit().get(2, TimeUnit.SECONDS); }
                    catch (Exception ignored) { /* Cleanup must not mask a completed screenshot or cancellation. */ }
                }
            }
            // This directory is a fresh browser profile owned by this capture.
            for (int attempt = 0; attempt < 5; attempt++) {
                try { FileUtil.del(profile.toFile()); break; }
                catch (RuntimeException locked) {
                    if (Thread.currentThread().isInterrupted()) break;
                    try { Thread.sleep(100); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
    }

    private static String findChrome() throws IOException {
        String configured = System.getenv("CHROME_PATH");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        List<Path> candidates = new ArrayList<>();
        for (String variable : List.of("PROGRAMFILES", "PROGRAMFILES(X86)", "LOCALAPPDATA")) {
            String base = System.getenv(variable);
            if (base != null) {
                candidates.add(Path.of(base, "Google", "Chrome", "Application", "chrome.exe"));
            }
        }
        candidates.addAll(List.of(Path.of("/usr/bin/google-chrome"), Path.of("/usr/bin/chromium"),
                Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toString();
            }
        }
        throw new IOException("Chrome not found. Install Chrome or set CHROME_PATH to its executable.");
    }
}
