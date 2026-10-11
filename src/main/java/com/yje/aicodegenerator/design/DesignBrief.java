package com.yje.aicodegenerator.design;

import java.util.List;

public record DesignBrief(String goal, String audience, List<Concept> concepts,
                          String selectedDirection, String layout, String typography,
                          List<String> palette, List<String> interactions, List<String> avoid) {
    public record Concept(String name, String composition, String informationOrganization,
                          String typography, String interaction) { }

    public void validate() {
        if (goal == null || goal.isBlank() || selectedDirection == null || selectedDirection.isBlank()
                || layout == null || layout.isBlank() || typography == null || typography.isBlank()
                || concepts == null || concepts.size() != 3 || concepts.stream().anyMatch(c -> c == null
                || c.name() == null || c.name().isBlank()) || palette == null || palette.isEmpty()) {
            throw new IllegalArgumentException("Design brief must contain a goal, three concepts and an executable direction");
        }
    }
}
