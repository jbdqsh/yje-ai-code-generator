package com.yje.aicodegenerator.design;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.imageio.ImageIO;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in paid integration check; isolated from user applications and chat history. */
@EnabledIfEnvironmentVariable(named = "DESIGN_LIVE_TEST", matches = "true")
class DesignLiveSmokeTest {
    @Test void generatesAndReviewsThreeDifferentBusinessCases() throws Exception {
        Path root = Path.of("target/verification/design-live");
        Files.createDirectories(root);
        DesignProperties properties = new DesignProperties();
        properties.setRoot(root.toString());
        properties.setBaseUrl(System.getenv("DESIGN_TEST_BASE_URL"));
        properties.setApiKey(System.getenv("OPENAI_API_KEY"));
        properties.setModelName("gpt-6.1-sol");
        properties.setTimeoutSeconds(180);
        var mapper = new ObjectMapper();
        var model = new DesignModelClient(properties, mapper);
        var resources = new SkillResources();
        var planner = new DesignPlanningService(properties, resources, model, mapper);
        var composer = new DesignPromptComposer(resources, mapper);
        var reviewer = new DesignReviewService(resources, model, mapper);
        boolean vision = model.probeVision();
        Files.writeString(root.resolve("vision.txt"), Boolean.toString(vision));
        List<String> requirements = List.of(
                "Build a sports equipment shop for runners. Prioritize product browsing, category filters and cart. Use real relevant images.",
                "Build a photography blog for travelers. Prioritize article discovery, legible reading and actual photographic imagery.",
                "Build an inventory operations dashboard for warehouse staff. Prioritize dense stock table, search, low-stock alerts and operations.");
        for (int i = 0; i < requirements.size(); i++) {
            var brief = planner.prepare(100 + i, requirements.get(i));
            Path folder = root.resolve(Integer.toString(100 + i));
            String html = model.chat("Return ONLY a compact complete working HTML document with inline CSS and JavaScript, at most 100 lines and 8000 characters. No Markdown fences. Build the primary business screen only.",
                    composer.compose(requirements.get(i), brief));
            if (html.startsWith("```")) html = html.substring(html.indexOf('\n') + 1, html.lastIndexOf("```"));
            assertTrue(html.toLowerCase().contains("<html"));
            Files.writeString(folder.resolve("index.html"), html);
            byte[] page = html.getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "text/html;charset=UTF-8");
                exchange.sendResponseHeaders(200, page.length);
                exchange.getResponseBody().write(page);
                exchange.close();
            });
            server.start();
            try {
                var screenshots = new DesignScreenshotService();
                String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
                Path desktop = folder.resolve("desktop.png"), mobile = folder.resolve("mobile.png");
                screenshots.capture(url, desktop, 1440, 900);
                screenshots.capture(url, mobile, 390, 844);
                assertEquals(1440, ImageIO.read(desktop.toFile()).getWidth());
                assertEquals(390, ImageIO.read(mobile.toFile()).getWidth());
                if (vision) mapper.writerWithDefaultPrettyPrinter().writeValue(folder.resolve("review.json").toFile(), reviewer.review(brief, List.of(desktop, mobile)));
            } finally { server.stop(0); }
        }
    }
}
