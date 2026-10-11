package com.yje.aicodegenerator.design;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DesignPromptComposer {
    private final SkillResources resources;
    private final ObjectMapper mapper;

    public DesignPromptComposer(SkillResources resources, ObjectMapper mapper) {
        this.resources = resources;
        this.mapper = mapper;
    }

    public String compose(String userMessage, DesignBrief brief) {
        try {
            return userMessage + "\n\n<design-guidance>\n" + resources.skill()
                    + "\nSelected executable design brief:\n" + mapper.writeValueAsString(brief)
                    + "\nImplement the selected direction in the current output type and existing technology stack. "
                    + "Preserve the system's required output format and tool protocol. This guidance applies to HTML, multi-file and Vue output. "
                    + "Build the actual business workflow, not a generic promotional landing page. "
                    + "Use relevant product/content imagery and inspect responsive text, contrast, spacing and interactive states. "
                    + "Do not display the brief, design process, skill names or implementation commentary inside the generated product. "
                    + "External orchestration performs screenshots and reviews; do not independently restart that review loop.\n</design-guidance>";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to compose design brief", e);
        }
    }
}
