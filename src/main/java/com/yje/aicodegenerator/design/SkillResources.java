package com.yje.aicodegenerator.design;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SkillResources {
    public String skill() { return read("design-skill/SKILL.md"); }
    public String reviewReference() { return read("design-skill/references/review-and-media.md"); }

    private String read(String name) {
        try (var input = new ClassPathResource(name).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Missing bundled design resource: " + name, e);
        }
    }
}
