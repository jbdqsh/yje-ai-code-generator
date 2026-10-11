package com.yje.aicodegenerator.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Chrome's CLI enforces a minimum layout width; CDP supplies the exact CSS viewport. */
final class ChromeViewportCapture implements WebSocket.Listener, AutoCloseable {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();
    private final StringBuilder incoming = new StringBuilder();
    private WebSocket socket;

    static void capture(Path profile, String url, Path output, int width, int height) throws Exception {
        Path portFile = profile.resolve("DevToolsActivePort");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!Files.exists(portFile) && System.nanoTime() < deadline) Thread.sleep(50);
        if (!Files.exists(portFile)) throw new IOException("Chrome debugger did not start");
        int port = Integer.parseInt(Files.readAllLines(portFile).getFirst());
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        try (var capture = new ChromeViewportCapture()) {
            String targets = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/json/list"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            String address = null;
            for (JsonNode target : capture.mapper.readTree(targets)) {
                if (target.path("type").asText().equals("page")) address = target.path("webSocketDebuggerUrl").asText();
            }
            if (address == null) throw new IOException("Chrome page target unavailable");
            capture.socket = http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(5))
                    .buildAsync(URI.create(address), capture).get(10, TimeUnit.SECONDS);
            capture.call("Emulation.setDeviceMetricsOverride", Map.of("width", width, "height", height,
                    "deviceScaleFactor", 1, "mobile", false));
            JsonNode navigation = capture.call("Page.navigate", Map.of("url", url));
            if (navigation.has("errorText")) throw new IOException("Chrome navigation failed");
            // Wait for fonts and images, but do not let an unavailable third-party image block delivery.
            capture.call("Runtime.evaluate", Map.of("expression",
                    "Promise.race([Promise.all([document.fonts.ready,new Promise(r=>{const p=()=>document.readyState==='complete'?r():setTimeout(p,50);p()})]),new Promise(r=>setTimeout(r,5000))])",
                    "awaitPromise", true));
            capture.call("Runtime.evaluate", Map.of("expression",
                    "(async()=>{await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));"
                            + "await Promise.race([Promise.all([...document.images].filter(i=>{const b=i.getBoundingClientRect();return b.top<innerHeight&&b.bottom>0}).map(i=>i.decode().catch(()=>{}))),new Promise(r=>setTimeout(r,8000))]);"
                            + "await new Promise(r=>setTimeout(r,250));})()", "awaitPromise", true));
            JsonNode viewport = capture.call("Runtime.evaluate", Map.of("expression", "window.innerWidth", "returnByValue", true));
            if (viewport.path("result").path("value").asInt() != width) throw new IOException("Chrome viewport mismatch");
            String base64 = capture.call("Page.captureScreenshot", Map.of("format", "png", "captureBeyondViewport", false))
                    .path("data").asText();
            Files.write(output, Base64.getDecoder().decode(base64));
        }
    }

    private JsonNode call(String method, Map<String, Object> parameters) throws Exception {
        int id = sequence.incrementAndGet();
        var response = new CompletableFuture<JsonNode>();
        pending.put(id, response);
        try {
            socket.sendText(mapper.writeValueAsString(Map.of("id", id, "method", method, "params", parameters)), true)
                    .get(5, TimeUnit.SECONDS);
            JsonNode message = response.get(15, TimeUnit.SECONDS);
            if (message.has("error")) throw new IOException("Chrome command failed: " + method);
            return message.path("result");
        } finally { pending.remove(id); }
    }

    @Override public void onOpen(WebSocket socket) { socket.request(1); }

    @Override public java.util.concurrent.CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        incoming.append(data);
        if (last) {
            try {
                JsonNode message = mapper.readTree(incoming.toString());
                var response = pending.get(message.path("id").asInt());
                if (response != null) response.complete(message);
            } catch (IOException error) { onError(socket, error); }
            incoming.setLength(0);
        }
        socket.request(1);
        return null;
    }

    @Override public void onError(WebSocket socket, Throwable error) {
        pending.values().forEach(response -> response.completeExceptionally(error));
    }

    @Override public void close() { if (socket != null) socket.abort(); }
}
