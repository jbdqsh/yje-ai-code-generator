package com.yje.aicodegenerator.design;

import com.yje.aicodegenerator.utils.ChromeScreenshotCapture;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

/** Checks the actual preview before capturing it, so error pages are not reviewed as designs. */
@Service
public class DesignScreenshotService {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public void capture(String url, Path output, int width, int height) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) throw new IOException("Preview unavailable (HTTP " + response.statusCode() + ")");
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        ChromeScreenshotCapture.capture(url, output, width, height);
    }
}
