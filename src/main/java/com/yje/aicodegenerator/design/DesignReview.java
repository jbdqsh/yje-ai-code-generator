package com.yje.aicodegenerator.design;

import java.util.List;

public record DesignReview(List<Issue> issues, String summary, boolean improvementNeeded) {
    public DesignReview {
        issues = issues == null ? List.of() : issues.stream().filter(java.util.Objects::nonNull).limit(3).toList();
        improvementNeeded = improvementNeeded && !issues.isEmpty();
    }

    public record Issue(String location, String problem, String impact, String suggestion) { }
}
