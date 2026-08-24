package com.example.chatdesktop.service;

import com.example.chatdesktop.config.GroqConfig;
import com.example.chatdesktop.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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

            String apiKey = GroqConfig.getApiKey();

            String json = criarRequest(historico);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GroqConfig.API_URL))
                    .timeout(Duration.ofSeconds(60))
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
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
                    .thenApply(this::processarResposta)
                    .exceptionallyCompose(erro -> {

                        Throwable causa = obterCausa(erro);

                        return CompletableFuture.failedFuture(
                                criarErroAmigavel(causa)
                        );
                    });

        } catch (Exception erro) {

            return CompletableFuture.failedFuture(
                    criarErroAmigavel(erro)
            );
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

        int status = response.statusCode();

        /*
         * ==========================================
         * RESPOSTA COM SUCESSO
         * ==========================================
         */

        if (status >= 200 && status < 300) {

            try {

                JsonObject json = JsonParser
                        .parseString(response.body())
                        .getAsJsonObject();

                JsonArray choices =
                        json.getAsJsonArray("choices");

                if (choices == null || choices.isEmpty()) {

                    throw new RuntimeException(
                            "A IA não retornou nenhuma resposta."
                    );
                }

                JsonObject choice =
                        choices
                                .get(0)
                                .getAsJsonObject();

                JsonObject message =
                        choice.getAsJsonObject("message");

                if (message == null
                        || !message.has("content")) {

                    throw new RuntimeException(
                            "A resposta da IA não possui conteúdo."
                    );
                }

                return message
                        .get("content")
                        .getAsString();

            } catch (Exception erro) {

                if (erro instanceof RuntimeException
                        && erro.getMessage() != null
                        && erro.getMessage().contains(
                        "A IA não retornou")) {

                    throw erro;
                }

                throw new RuntimeException(
                        "Não foi possível interpretar "
                                + "a resposta da IA."
                );
            }
        }

        /*
         * ==========================================
         * ERROS HTTP
         * ==========================================
         */

        throw new GroqApiException(
                status,
                obterMensagemPorStatus(status)
        );
    }

    private String obterMensagemPorStatus(int status) {

        return switch (status) {

            /*
             * 400
             */
            case 400 ->
                    "A solicitação enviada para a IA "
                            + "é inválida.";

            /*
             * 401
             */
            case 401 ->
                    "A chave da API da Groq é inválida "
                            + "ou não foi autorizada.";

            /*
             * 403
             */
            case 403 ->
                    "Acesso à API não autorizado. "
                            + "Verifique sua chave da Groq.";

            /*
             * 404
             */
            case 404 ->
                    "O modelo da IA não foi encontrado. "
                            + "Verifique a configuração do modelo.";

            /*
             * 408
             */
            case 408 ->
                    "A solicitação demorou muito para responder. "
                            + "Tente novamente.";

            /*
             * 429
             */
            case 429 ->
                    "O limite de requisições da API foi atingido.\n\n"
                            + "Aguarde alguns instantes e tente novamente.";

            /*
             * 500
             */
            case 500 ->
                    "O servidor da Groq apresentou um erro interno.\n\n"
                            + "Tente novamente em alguns instantes.";

            /*
             * 502
             */
            case 502 ->
                    "O servidor da Groq está temporariamente "
                            + "indisponível.\n\n"
                            + "Tente novamente mais tarde.";

            /*
             * 503
             */
            case 503 ->
                    "O serviço da Groq está temporariamente "
                            + "indisponível.\n\n"
                            + "Tente novamente mais tarde.";

            /*
             * 504
             */
            case 504 ->
                    "O servidor demorou muito para responder.\n\n"
                            + "Tente novamente.";

            /*
             * Outros códigos
             */
            default ->
                    "Não foi possível comunicar com a API da Groq.\n\n"
                            + "Código do erro: " + status;
        };
    }

    private Throwable obterCausa(Throwable erro) {

        Throwable atual = erro;

        while (atual instanceof CompletionException
                && atual.getCause() != null) {

            atual = atual.getCause();
        }

        return atual;
    }

    private RuntimeException criarErroAmigavel(
            Throwable erro
    ) {

        /*
         * ==========================================
         * ERRO DA API
         * ==========================================
         */

        if (erro instanceof GroqApiException) {

            return (GroqApiException) erro;
        }

        /*
         * ==========================================
         * SEM INTERNET
         * ==========================================
         */

        if (erro instanceof UnknownHostException) {

            return new RuntimeException(
                    "🌐 Não foi possível acessar a internet.\n\n"
                            + "Verifique sua conexão com a internet "
                            + "e tente novamente."
            );
        }

        /*
         * ==========================================
         * FALHA DE CONEXÃO
         * ==========================================
         */

        if (erro instanceof ConnectException) {

            return new RuntimeException(
                    "📡 Não foi possível conectar ao servidor da IA.\n\n"
                            + "Verifique sua conexão com a internet "
                            + "e tente novamente."
            );
        }

        /*
         * ==========================================
         * TIMEOUT
         * ==========================================
         */

        if (erro instanceof HttpTimeoutException) {

            return new RuntimeException(
                    "⏱️ A comunicação com a IA demorou muito.\n\n"
                            + "Verifique sua conexão e tente novamente."
            );
        }

        /*
         * ==========================================
         * OUTRAS FALHAS DE INTERNET
         * ==========================================
         */

        if (erro instanceof IOException) {

            return new RuntimeException(
                    "📡 Ocorreu uma falha de comunicação com a IA.\n\n"
                            + "Verifique sua internet e tente novamente."
            );
        }

        /*
         * ==========================================
         * API KEY NÃO CONFIGURADA
         * ==========================================
         */

        if (erro instanceof IllegalStateException
                && erro.getMessage() != null
                && erro.getMessage().contains(
                "GROQ_API_KEY")) {

            return new RuntimeException(
                    "🔑 A chave da API da Groq "
                            + "não foi configurada.\n\n"
                            + "Configure a variável de ambiente "
                            + "GROQ_API_KEY e reinicie a aplicação."
            );
        }

        /*
         * ==========================================
         * ERRO DESCONHECIDO
         * ==========================================
         */

        return new RuntimeException(
                "❌ Ocorreu um erro ao comunicar com a IA.\n\n"
                        + "Tente novamente."
        );
    }

    /*
     * ==============================================
     * EXCEÇÃO ESPECÍFICA DA API GROQ
     * ==============================================
     */

    private static class GroqApiException
            extends RuntimeException {

        private final int statusCode;

        public GroqApiException(
                int statusCode,
                String message
        ) {

            super(message);

            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}