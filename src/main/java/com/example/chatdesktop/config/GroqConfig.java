package com.example.chatdesktop.config;

public final class GroqConfig {

    private GroqConfig() {
    }

    public static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // IA utilizada quando o RAG encontra informação
    public static final String MODEL =
            "openai/gpt-oss-20b";

    // IA utilizada quando o RAG NÃO encontra informação
    // Possui Web Search integrado
    public static final String WEB_MODEL =
            "groq/compound-mini";

    public static String getApiKey() {

        String apiKey = System.getenv("GROQ_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {

            throw new IllegalStateException(
                    "A variável de ambiente GROQ_API_KEY não foi configurada."
            );
        }

        return apiKey;
    }
}