package com.yje.aicodegenerator.service;

import com.sun.net.httpserver.HttpServer;
import com.yje.aicodegenerator.utils.WebScreenshotUtils;
import com.yje.aicodegenerator.utils.ChromeScreenshotCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "screenshot.smoke", matches = "true")
class ChromeScreenshotSmokeTest {
    @Test
    void capturesLocalWebsiteWithRealChrome() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] html = ("<html><body style='background:#fff;color:#111'>"
                    + "<h1>YJE local screenshot verification</h1></body></html>")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html;charset=UTF-8");
            exchange.sendResponseHeaders(200, html.length);
            try (var output = exchange.getResponseBody()) {
                output.write(html);
            }
        });
        server.start();
        try {
            String capture = WebScreenshotUtils.saveWebPageScreenshot(
                    "http://localhost:" + server.getAddress().getPort() + "/");
            assertNotNull(capture, "Chrome screenshot failed");
            var image = ImageIO.read(Path.of(capture).toFile());
            assertNotNull(image);
            assertTrue(image.getWidth() > 1000);
            boolean hasContent = false;
            for (int y = 15; y < 80; y += 2) {
                for (int x = 10; x < 600; x += 2) {
                    hasContent |= image.getRGB(x, y) != image.getRGB(0, 0);
                }
            }
            assertTrue(hasContent, "Screenshot must contain rendered text");
            Path artifact = Path.of("target/verification/cover.jpg");
            Files.createDirectories(artifact.getParent());
            Files.move(Path.of(capture), artifact, StandardCopyOption.REPLACE_EXISTING);
            for (int[] viewport : new int[][]{{1440, 900}, {390, 844}}) {
                Path png = artifact.resolveSibling("viewport-" + viewport[0] + ".png");
                ChromeScreenshotCapture.capture("http://localhost:" + server.getAddress().getPort() + "/", png, viewport[0], viewport[1]);
                var screenshot = ImageIO.read(png.toFile());
                assertEquals(viewport[0], screenshot.getWidth());
                assertEquals(viewport[1], screenshot.getHeight());
            }
        } finally {
            server.stop(0);
        }
    }
}
