package com.yje.aicodegenerator.design;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "design")
public class DesignProperties {
    private boolean enabled = true;
    private boolean reviewEnabled = true;
    private int maxRefinements = 2;
    private String baseUrl;
    @lombok.ToString.Exclude
    private String apiKey;
    private String modelName = "gpt-6.1-sol";
    private int timeoutSeconds = 90;
    private String root = System.getProperty("user.dir") + "/tmp/design";
    private String previewBaseUrl = "http://localhost:8123/api/static";
}
