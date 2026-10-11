package com.yje.aicodegenerator.design;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DesignModelClientTest {
    @Test void transientProbeFailureIsRetriedAndSemanticSuccessCached() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            int count = requests.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("data:image/png;base64,"));
            byte[] response = (count == 1 ? "temporary outage" : "{\"choices\":[{\"message\":{\"content\":\"The left is red and the right is blue.\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(count == 1 ? 503 : 200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            DesignProperties properties = new DesignProperties();
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/");
            properties.setApiKey("test-key");
            DesignModelClient client = new DesignModelClient(properties, new ObjectMapper());
            assertThrows(RuntimeException.class, client::probeVision);
            assertTrue(client.probeVision());
            assertTrue(client.probeVision());
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void acceptsFencedJsonButRejectsInvalidJson() {
        var client = new DesignModelClient(new DesignProperties(), new ObjectMapper());
        assertFalse(client.parse("```json\n{\"issues\":[],\"summary\":\"Fine\",\"improvementNeeded\":false}\n```", DesignReview.class).improvementNeeded());
        assertThrows(IllegalArgumentException.class, () -> client.parse("a webpage", DesignReview.class));
    }
}
