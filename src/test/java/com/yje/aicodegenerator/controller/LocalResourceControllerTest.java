package com.yje.aicodegenerator.controller;

import com.yje.aicodegenerator.service.impl.LocalFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LocalResourceControllerTest {
    @TempDir
    Path directory;
    private LocalFileStorageService storage;
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        storage = new LocalFileStorageService();
        ReflectionTestUtils.setField(storage, "root", directory.resolve("uploads"));
        ReflectionTestUtils.setField(storage, "publicBaseUrl", "http://localhost:8123/api/files");
        storage.initialize();
        LocalResourceController controller = new LocalResourceController(storage);
        ReflectionTestUtils.setField(controller, "deployRoot", directory.resolve("deploy"));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadedFilesPersistAndAreServed() throws Exception {
        Path source = directory.resolve("screenshot.jpg");
        Files.write(source, new byte[]{1, 2, 3});
        String url = storage.save("/screenshots/test.jpg", source.toFile());
        assertEquals("http://localhost:8123/api/files/screenshots/test.jpg", url);
        Files.delete(source);
        mvc.perform(get("/files/screenshots/test.jpg"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void missingFilesAndTraversalAreRejected() throws Exception {
        mvc.perform(get("/files/missing.jpg")).andExpect(status().isNotFound());
        Path source = Files.writeString(directory.resolve("secret.txt"), "secret");
        assertThrows(IllegalArgumentException.class,
                () -> storage.save("../outside.txt", source.toFile()));
        assertThrows(IllegalArgumentException.class,
                () -> storage.save("..\\outside.txt", source.toFile()));
        mvc.perform(get("/files/../secret.txt")).andExpect(status().isNotFound());
    }

    @Test
    void deployedDirectoryRedirectsToTrailingSlash() throws Exception {
        mvc.perform(get("/deploy/sample")).andExpect(status().isFound())
                .andExpect(header().string("Location", "/deploy/sample/"));
        mvc.perform(get("/deploy/missing/asset.js")).andExpect(status().isNotFound());
    }

    @Test
    void servesDeploymentIndexAndNestedAssets() throws Exception {
        Path site = directory.resolve("deploy/sample");
        Files.createDirectories(site.resolve("assets"));
        Files.writeString(site.resolve("index.html"), "<html>site</html>");
        Files.writeString(site.resolve("assets/site.js"), "console.log('site')");
        mvc.perform(get("/deploy/sample/"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string("<html>site</html>"));
        mvc.perform(get("/deploy/sample/assets/site.js"))
                .andExpect(status().isOk()).andExpect(content().string("console.log('site')"));
        mvc.perform(get("/deploy/sample/../other/index.html")).andExpect(status().isNotFound());
    }
}
