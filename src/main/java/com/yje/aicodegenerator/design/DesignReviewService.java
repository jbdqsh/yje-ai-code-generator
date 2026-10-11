package com.yje.aicodegenerator.design;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.List;

@Service
public class DesignReviewService {
    private final SkillResources resources;
    private final DesignModelClient model;
    private final ObjectMapper mapper;

    public DesignReviewService(SkillResources resources, DesignModelClient model, ObjectMapper mapper) {
        this.resources = resources;
        this.model = model;
        this.mapper = mapper;
    }

    public boolean probeVision() { return model.probeVision(); }

    public DesignReview review(DesignBrief brief, List<Path> pngs) {
        if (!probeVision()) throw new IllegalStateException("Visual review unavailable: model image input unsupported");
        try {
            String prompt = resources.reviewReference() + "\nReview the attached actual desktop and mobile screenshots as an independent reviewer. "
                    + "Evaluate business relevance, composition, hierarchy, typography, imagery, readability and narrow-screen layout. "
                    + "Judge visible evidence only. Screenshots do not establish functional correctness. "
                    + "Do not assign a score. Identify at most three actionable high-impact issues. "
                    + "Product goal and design brief: " + mapper.writeValueAsString(brief)
                    + "\nReturn ONLY JSON: {\"issues\":[{\"location\":\"\",\"problem\":\"\",\"impact\":\"\",\"suggestion\":\"\"}],"
                    + "\"summary\":\"\",\"improvementNeeded\":true}. Set improvementNeeded=false when no material visible issues remain.";
            return model.parse(model.review(prompt, pngs), DesignReview.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to prepare visual review", e);
        }
    }
}
