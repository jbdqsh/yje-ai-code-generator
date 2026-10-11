package dev.langchain4j.service;

final class ToolResultNormalizer {

    private ToolResultNormalizer() {
    }

    static String normalize(String result) {
        return result == null || result.isBlank()
                ? "Tool returned no output; this does not confirm that the operation succeeded."
                : result;
    }
}
