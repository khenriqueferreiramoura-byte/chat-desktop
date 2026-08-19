package com.example.chatdesktop.service;

import com.example.chatdesktop.config.GroqConfig;
import com.example.chatdesktop.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GroqService {

    private final HttpClient httpClient;
    private final Gson gson;

    public GroqService() {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        this.gson = new Gson();
    }

    public CompletableFuture<String> enviarMensagem(
            List<ChatMessage> historico
    ) {

        try {

            String json = criarRequest(historico);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GroqConfig.API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header(
                            "Authorization",
                            "Bearer " + GroqConfig.getApiKey()
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(json)
                    )
                    .build();

            return httpClient
                    .sendAsync(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .thenApply(this::processarResposta);

        } catch (Exception erro) {

            return CompletableFuture.failedFuture(erro);
        }
    }

    private String criarRequest(
            List<ChatMessage> historico
    ) {

        JsonObject request = new JsonObject();

        request.addProperty(
                "model",
                GroqConfig.MODEL
        );

        JsonArray messages = new JsonArray();

        for (ChatMessage mensagem : historico) {

            JsonObject message = new JsonObject();

            message.addProperty(
                    "role",
                    mensagem.getRole()
            );

            message.addProperty(
                    "content",
                    mensagem.getContent()
            );

            messages.add(message);
        }

        request.add(
                "messages",
                messages
        );

        return gson.toJson(request);
    }

    private String processarResposta(
            HttpResponse<String> response
    ) {

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new RuntimeException(
                    "Erro ao chamar a Groq. "
                            + "Status: "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        JsonObject json = JsonParser
                .parseString(response.body())
                .getAsJsonObject();

        JsonArray choices =
                json.getAsJsonArray("choices");

        if (choices == null || choices.isEmpty()) {

            throw new RuntimeException(
                    "A Groq não retornou nenhuma resposta."
            );
        }

        JsonObject choice =
                choices.get(0)
                        .getAsJsonObject();

        JsonObject message =
                choice.getAsJsonObject("message");

        if (message == null
                || !message.has("content")) {

            throw new RuntimeException(
                    "A resposta da Groq não possui conteúdo."
            );
        }

        return message
                .get("content")
                .getAsString();
    }
}