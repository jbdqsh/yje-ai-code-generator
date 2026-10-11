package dev.langchain4j.service;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultNormalizerTest {

    @Test
    void emptyResultsCanBeAddedToToolMessages() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1").name("readFile").arguments("{}").build();
        for (String result : new String[]{null, "", " \n\t"}) {
            String normalized = ToolResultNormalizer.normalize(result);
            assertFalse(normalized.isBlank());
            assertTrue(normalized.contains("does not confirm"));
            assertDoesNotThrow(() -> ToolExecutionResultMessage.from(request, normalized));
        }
    }

    @Test
    void preservesNonEmptyToolResults() {
        assertEquals("file contents\n", ToolResultNormalizer.normalize("file contents\n"));
        assertEquals("Error: file missing", ToolResultNormalizer.normalize("Error: file missing"));
    }
}
