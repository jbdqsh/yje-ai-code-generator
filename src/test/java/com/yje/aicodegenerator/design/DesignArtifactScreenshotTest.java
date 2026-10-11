package com.yje.aicodegenerator.design;

import com.yje.aicodegenerator.utils.ChromeScreenshotCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Rechecks existing paid-test output without making any additional model calls. */
@EnabledIfSystemProperty(named = "design.artifacts", matches = "true")
class DesignArtifactScreenshotTest {
    @Test void recapturesGeneratedPagesAtExactCssViewports() throws Exception {
        Path root = Path.of("target/verification/design-live");
        int pages = 0;
        try (var folders = Files.list(root)) {
            for (Path folder : folders.filter(Files::isDirectory).toList()) {
                Path html = folder.resolve("index.html");
                if (!Files.exists(html)) continue;
                pages++;
                for (int[] size : new int[][]{{1440, 900}, {390, 844}}) {
                    Path image = folder.resolve("viewport-" + size[0] + ".png");
                    ChromeScreenshotCapture.capture(html.toAbsolutePath().toUri().toString(), image, size[0], size[1]);
                    var bitmap = ImageIO.read(image.toFile());
                    assertEquals(size[0], bitmap.getWidth());
                    assertEquals(size[1], bitmap.getHeight());
                }
            }
        }
        assertTrue(pages > 0, "Run the paid integration test to create sample pages first");
    }
}
