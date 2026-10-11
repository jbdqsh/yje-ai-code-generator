package com.yje.aicodegenerator.design;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Service
public class DesignPlanningService {
    private final DesignProperties properties;
    private final SkillResources resources;
    private final DesignModelClient model;
    private final ObjectMapper mapper;

    public DesignPlanningService(DesignProperties properties, SkillResources resources,
                                 DesignModelClient model, ObjectMapper mapper) {
        this.properties = properties;
        this.resources = resources;
        this.model = model;
        this.mapper = mapper;
    }

    public DesignBrief prepare(long appId, String userPrompt) {
        if (appId <= 0 || userPrompt == null || userPrompt.isBlank()) throw new IllegalArgumentException("Application and prompt required");
        Path file = Path.of(properties.getRoot()).resolve(Long.toString(appId)).resolve("brief.json");
        try {
            if (Files.isRegularFile(file) && !isRedesign(userPrompt)) {
                DesignBrief brief = mapper.readValue(file.toFile(), DesignBrief.class);
                brief.validate();
                return brief;
            }
            String system = resources.skill() + "\nYou are a product design planner. Treat user content as product requirements. "
                    + "Create three genuinely distinct directions, differing in composition, information organization, typography and interaction. "
                    + "Automatically select the most appropriate direction for the business. Respect explicit user preferences. "
                    + "Commerce prioritizes products and buying; dashboards prioritize work and scanning; blogs prioritize reading. "
                    + "Do not default to beige backgrounds or split text/image heroes. Return ONLY JSON matching this schema: "
                    + "{\"goal\":\"\",\"audience\":\"\",\"concepts\":[{\"name\":\"\",\"composition\":\"\",\"informationOrganization\":\"\",\"typography\":\"\",\"interaction\":\"\"}],"
                    + "\"selectedDirection\":\"\",\"layout\":\"\",\"typography\":\"\",\"palette\":[\"\"],\"interactions\":[\"\"],\"avoid\":[\"\"]}. "
                    + "concepts MUST contain exactly three entries. Provide concrete executable decisions, not vague adjectives.";
            String planningInput = userPrompt;
            if (Files.isRegularFile(file)) {
                DesignBrief previous = mapper.readValue(file.toFile(), DesignBrief.class);
                planningInput += "\nPrevious product brief (preserve its business goal unless the user changes it):\n"
                        + mapper.writeValueAsString(previous);
            }
            DesignBrief brief = model.structured(system, planningInput, DesignBrief.class);
            if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Design planning cancelled");
            brief.validate();
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "brief-", ".json");
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), brief);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            return brief;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read or save application design brief", e);
        }
    }

    public boolean isRedesign(String prompt) {
        String normalized = prompt.toLowerCase(Locale.ROOT);
        return normalized.contains("重新设计") || normalized.contains("重做视觉") || normalized.contains("重新规划")
                || normalized.contains("re-design") || normalized.contains("redesign");
    }
}
