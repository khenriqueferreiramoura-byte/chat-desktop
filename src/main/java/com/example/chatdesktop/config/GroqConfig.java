package com.example.chatdesktop.config;

public final class GroqConfig {

    private GroqConfig() {
    }

    public static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    public static final String MODEL =
            "openai/gpt-oss-20b";

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