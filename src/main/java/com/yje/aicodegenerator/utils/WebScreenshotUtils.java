package com.yje.aicodegenerator.utils;

import cn.hutool.core.img.ImgUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
public final class WebScreenshotUtils {
    private WebScreenshotUtils() {
    }

    public static String saveWebPageScreenshot(String webUrl) {
        if (webUrl == null || webUrl.isBlank()) {
            return null;
        }
        Path directory = Path.of(System.getProperty("user.dir"), "tmp", "screenshots", UUID.randomUUID().toString());
        Path raw = directory.resolve("capture.png");
        try {
            Files.createDirectories(directory);
            ChromeScreenshotCapture.capture(webUrl, raw);
            Path compressed = directory.resolve("cover.jpg");
            ImgUtil.compress(raw.toFile(), compressed.toFile(), 0.7f);
            return compressed.toString();
        } catch (Exception error) {
            log.error("网页截图失败：{}", webUrl, error);
            return null;
        } finally {
            try {
                Files.deleteIfExists(raw);
            } catch (Exception error) {
                log.warn("清理原始截图失败", error);
            }
        }
    }
}
