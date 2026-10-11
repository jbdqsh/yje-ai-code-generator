package com.yje.aicodegenerator.design;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Independent stateless requests, separate from generation chat memory. */
@Component
public class DesignModelClient {
    private final DesignProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private volatile Boolean visionSupported;
    private volatile String probedConfiguration;

    public DesignModelClient(DesignProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    public String chat(String system, String user) {
        return request(List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
    }

    public <T> T structured(String system, String user, Class<T> type) {
        return parse(chat(system, user), type);
    }

    public <T> T parse(String response, Class<T> type) {
        String json = response.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start < 0 || end <= start) throw new IllegalArgumentException("Invalid fenced model response");
            json = json.substring(start + 1, end).trim();
        }
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Design model returned invalid structured JSON", e);
        }
    }

    public String review(String prompt, List<Path> images) {
        if (images.isEmpty()) throw new IllegalArgumentException("Visual review requires screenshots");
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        try {
            for (Path image : images) content.add(imageContent(Files.readAllBytes(image)));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read review screenshots", e);
        }
        return request(List.of(Map.of("role", "user", "content", content)));
    }

    public synchronized boolean probeVision() {
        String configuration = properties.getBaseUrl() + "|" + properties.getModelName();
        if (configuration.equals(probedConfiguration) && visionSupported != null) return visionSupported;
        try {
            BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 32; y++) for (int x = 0; x < 64; x++)
                image.setRGB(x, y, x < 32 ? 0xff0000 : 0x0000ff);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            String response = request(List.of(Map.of("role", "user", "content", List.of(
                    Map.of("type", "text", "text", "Describe the colors and their positions in this image in English. Be brief."),
                    imageContent(bytes.toByteArray()))))).toLowerCase(Locale.ROOT);
            boolean valid = response.contains("red") && response.contains("blue")
                    && response.contains("left") && response.contains("right");
            visionSupported = valid;
            probedConfiguration = configuration;
            return valid;
        } catch (ModelHttpException e) {
            String body = e.responseBody.toLowerCase(Locale.ROOT);
            if ((e.status == 400 || e.status == 422) && (body.contains("image") || body.contains("vision"))
                    && (body.contains("support") || body.contains("unsupported"))) {
                visionSupported = false;
                probedConfiguration = configuration;
                return false;
            }
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create vision capability probe", e);
        }
    }

    private Map<String, Object> imageContent(byte[] image) {
        return Map.of("type", "image_url", "image_url", Map.of("url", "data:image/png;base64," + Base64.getEncoder().encodeToString(image)));
    }

    private String request(List<Map<String, Object>> messages) {
        if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Design model request cancelled");
        if (properties.getBaseUrl() == null || properties.getApiKey() == null || properties.getApiKey().isBlank())
            throw new IllegalStateException("Design model endpoint and API key must be configured");
        try {
            String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            String body = mapper.writeValueAsString(Map.of("model", properties.getModelName(), "messages", messages, "stream", false));
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new ModelHttpException(response.statusCode(), response.body());
            var content = mapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) throw new IllegalStateException("Design model returned no text content");
            return content.asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Design model request cancelled", e);
        } catch (IOException e) {
            throw new IllegalStateException("Design model request failed", e);
        }
    }

    private static class ModelHttpException extends RuntimeException {
        final int status;
        final String responseBody;
        ModelHttpException(int status, String body) {
            super("Design model HTTP request failed (status " + status + ")");
            this.status = status;
            this.responseBody = body;
        }
    }
}
