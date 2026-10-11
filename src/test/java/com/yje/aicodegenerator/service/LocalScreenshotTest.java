package com.yje.aicodegenerator.service;

import com.yje.aicodegenerator.service.impl.LocalFileStorageService;
import com.yje.aicodegenerator.service.impl.ScreenshotServiceImpl;
import com.yje.aicodegenerator.utils.WebScreenshotUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalScreenshotTest {
    @TempDir
    Path directory;

    @Test
    void savesCoverLocallyBeforeDeletingTemporaryScreenshot() throws Exception {
        Path screenshot = Files.write(directory.resolve("capture.jpg"), new byte[]{1, 2, 3});
        LocalFileStorageService storage = new LocalFileStorageService();
        ReflectionTestUtils.setField(storage, "root", directory.resolve("uploads"));
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "http://localhost:8123/api/files");
        storage.initialize();
        ScreenshotServiceImpl service = new ScreenshotServiceImpl();
        ReflectionTestUtils.setField(service, "fileStorageService", storage);
        try (var screenshots = mockStatic(WebScreenshotUtils.class)) {
            screenshots.when(() -> WebScreenshotUtils.saveWebPageScreenshot("http://localhost/site/"))
                    .thenReturn(screenshot.toString());
            String url = service.generateAndUploadScreenshot("http://localhost/site/");
            String key = url.substring("http://localhost:8123/api/files/".length());
            assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(storage.root().resolve(key)));
            assertFalse(Files.exists(screenshot));
        }
    }
}
