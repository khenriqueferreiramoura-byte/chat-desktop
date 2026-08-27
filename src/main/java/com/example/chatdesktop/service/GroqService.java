package com.example.chatdesktop.service;

import com.example.chatdesktop.config.GroqConfig;
import com.example.chatdesktop.model.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
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

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(20)
                        )
                        .build();

        this.gson = new Gson();
    }

    // =====================================================
    // CHAT NORMAL / RAG
    // =====================================================

    public CompletableFuture<String> enviarMensagem(
            List<ChatMessage> historico
    ) {

        try {

            String apiKey =
                    GroqConfig.getApiKey();

            String json =
                    criarRequest(
                            historico,
                            GroqConfig.MODEL
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            GroqConfig.API_URL
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(60)
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(json)
                            )
                            .build();

            return httpClient
                    .sendAsync(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .thenApply(
                            this::processarResposta
                    )
                    .exceptionallyCompose(
                            erro -> {

                                Throwable causa =
                                        obterCausa(erro);

                                return CompletableFuture
                                        .failedFuture(
                                                criarErroAmigavel(
                                                        causa
                                                )
                                        );
                            }
                    );

        } catch (Exception erro) {

            return CompletableFuture.failedFuture(
                    criarErroAmigavel(erro)
            );
        }
    }

    // =====================================================
    // PESQUISA NA INTERNET
    // =====================================================

    public CompletableFuture<String> pesquisarNaInternet(
            String pergunta
    ) {

        if (pergunta == null ||
                pergunta.isBlank()) {

            return CompletableFuture.completedFuture(
                    "Não foi possível realizar a pesquisa."
            );
        }

        try {

            String apiKey =
                    GroqConfig.getApiKey();

            /*
             * =================================================
             * SYSTEM PROMPT
             * =================================================
             */

            ChatMessage system =
                    new ChatMessage(
                            "system",
                            """
                            Você é a Orbit-IA.

                            A base de conhecimento local não encontrou
                            informações suficientemente relevantes para
                            responder à pergunta.

                            Agora utilize a pesquisa na internet para
                            encontrar informações atuais e relevantes.

                            REGRAS:

                            - Responda em português do Brasil.
                            - Faça uma pesquisa superficial e objetiva.
                            - Priorize fontes confiáveis.
                            - Prefira informações recentes quando a pergunta
                              depender de atualidade.
                            - Não invente informações.
                            - Não apresente especulações como fatos.
                            - Utilize as informações encontradas na pesquisa.
                            - Se as fontes não forem suficientes, diga isso.
                            - Seja objetivo.
                            - Não faça uma pesquisa excessivamente profunda.
                            - Responda diretamente à pergunta do usuário.

                            A pesquisa web deve ser utilizada para complementar
                            a resposta quando a base local não possuir
                            informações suficientes.
                            """
                    );

            ChatMessage user =
                    new ChatMessage(
                            "user",
                            pergunta
                    );

            List<ChatMessage> mensagens =
                    List.of(
                            system,
                            user
                    );

            /*
             * =================================================
             * REQUEST COM COMPOUND MINI
             * =================================================
             */

            String json =
                    criarRequestWeb(
                            mensagens
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            GroqConfig.API_URL
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(90)
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .header(
                                    "Groq-Model-Version",
                                    "latest"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(json)
                            )
                            .build();

            System.out.println();
            System.out.println(
                    "========================================"
            );
            System.out.println(
                    "🌐 PESQUISA WEB ATIVADA"
            );
            System.out.println(
                    "Modelo: "
                            + GroqConfig.WEB_MODEL
            );
            System.out.println(
                    "Pergunta: "
                            + pergunta
            );
            System.out.println(
                    "========================================"
            );

            return httpClient
                    .sendAsync(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .thenApply(
                            response -> {

                                System.out.println(
                                        "🌐 Resposta recebida da pesquisa web."
                                );

                                return processarResposta(
                                        response
                                );
                            }
                    )
                    .exceptionallyCompose(
                            erro -> {

                                Throwable causa =
                                        obterCausa(erro);

                                return CompletableFuture
                                        .failedFuture(
                                                criarErroAmigavel(
                                                        causa
                                                )
                                        );
                            }
                    );

        } catch (Exception erro) {

            return CompletableFuture.failedFuture(
                    criarErroAmigavel(erro)
            );
        }
    }

    // =====================================================
    // REQUEST NORMAL
    // =====================================================

    private String criarRequest(
            List<ChatMessage> historico,
            String modelo
    ) {

        JsonObject request =
                new JsonObject();

        request.addProperty(
                "model",
                modelo
        );

        JsonArray messages =
                criarMensagensJson(
                        historico
                );

        request.add(
                "messages",
                messages
        );

        return gson.toJson(request);
    }

    // =====================================================
    // REQUEST WEB
    // =====================================================

    private String criarRequestWeb(
            List<ChatMessage> mensagens
    ) {

        JsonObject request =
                new JsonObject();

        /*
         * Modelo Compound Mini
         */
        request.addProperty(
                "model",
                GroqConfig.WEB_MODEL
        );

        /*
         * Mensagens
         */
        request.add(
                "messages",
                criarMensagensJson(
                        mensagens
                )
        );

        /*
         * =================================================
         * COMPOUND CUSTOM
         * =================================================
         */

        JsonObject compoundCustom =
                new JsonObject();

        JsonObject tools =
                new JsonObject();

        JsonArray enabledTools =
                new JsonArray();

        /*
         * Somente pesquisa web.
         *
         * Não habilitamos:
         *
         * code_interpreter
         * visit_website
         * wolfram_alpha
         */

        enabledTools.add(
                "web_search"
        );

        tools.add(
                "enabled_tools",
                enabledTools
        );

        compoundCustom.add(
                "tools",
                tools
        );

        request.add(
                "compound_custom",
                compoundCustom
        );

        /*
         * =================================================
         * CONFIGURAÇÃO DA PESQUISA
         * =================================================
         */

        JsonObject searchSettings =
                new JsonObject();

        /*
         * Prioriza resultados brasileiros.
         */
        searchSettings.addProperty(
                "country",
                "brazil"
        );

        /*
         * Mantém a pesquisa ampla.
         */
        request.add(
                "search_settings",
                searchSettings
        );

        return gson.toJson(request);
    }

    // =====================================================
    // MENSAGENS JSON
    // =====================================================

    private JsonArray criarMensagensJson(
            List<ChatMessage> mensagens
    ) {

        JsonArray messages =
                new JsonArray();

        for (ChatMessage mensagem :
                mensagens) {

            JsonObject message =
                    new JsonObject();

            message.addProperty(
                    "role",
                    mensagem.getRole()
            );

            message.addProperty(
                    "content",
                    mensagem.getContent()
            );

            messages.add(
                    message
            );
        }

        return messages;
    }

    // =====================================================
    // PROCESSAR RESPOSTA
    // =====================================================

    private String processarResposta(
            HttpResponse<String> response
    ) {

        int status =
                response.statusCode();

        System.out.println(
                "HTTP status: "
                        + status
        );

        if (status >= 200 &&
                status < 300) {

            try {

                JsonObject json =
                        JsonParser
                                .parseString(
                                        response.body()
                                )
                                .getAsJsonObject();

                JsonArray choices =
                        json.getAsJsonArray(
                                "choices"
                        );

                if (choices == null ||
                        choices.isEmpty()) {

                    throw new RuntimeException(
                            "A IA não retornou nenhuma resposta."
                    );
                }

                JsonObject choice =
                        choices
                                .get(0)
                                .getAsJsonObject();

                JsonObject message =
                        choice.getAsJsonObject(
                                "message"
                        );

                if (message == null ||
                        !message.has("content")) {

                    throw new RuntimeException(
                            "A resposta da IA não possui conteúdo."
                    );
                }

                String resposta =
                        message
                                .get("content")
                                .getAsString();

                if (resposta == null ||
                        resposta.isBlank()) {

                    throw new RuntimeException(
                            "A IA retornou uma resposta vazia."
                    );
                }

                return resposta.trim();

            } catch (Exception erro) {

                if (erro instanceof RuntimeException
                        && erro.getMessage() != null
                        && (
                        erro.getMessage().contains(
                                "A IA não retornou"
                        )
                                ||
                                erro.getMessage().contains(
                                        "A resposta da IA"
                                )
                )) {

                    throw erro;
                }

                System.err.println(
                        "Resposta recebida:"
                );

                System.err.println(
                        response.body()
                );

                throw new RuntimeException(
                        "Não foi possível interpretar "
                                + "a resposta da IA."
                );
            }
        }

        throw new GroqApiException(
                status,
                obterMensagemPorStatus(
                        status
                )
        );
    }

    // =====================================================
    // STATUS HTTP
    // =====================================================

    private String obterMensagemPorStatus(
            int status
    ) {

        return switch (status) {

            case 400 ->
                    "A solicitação enviada para a IA é inválida.";

            case 401 ->
                    "A chave da API da Groq é inválida ou não foi autorizada.";

            case 403 ->
                    "Acesso à API não autorizado. Verifique sua chave da Groq.";

            case 404 ->
                    "O modelo da IA não foi encontrado.";

            case 408 ->
                    "A solicitação demorou muito para responder.";

            case 429 ->
                    "O limite de requisições da API foi atingido.\n\n"
                            + "Aguarde alguns instantes e tente novamente.";

            case 500 ->
                    "O servidor da Groq apresentou um erro interno.";

            case 502 ->
                    "O servidor da Groq está temporariamente indisponível.";

            case 503 ->
                    "O serviço da Groq está temporariamente indisponível.";

            case 504 ->
                    "O servidor demorou muito para responder.";

            default ->
                    "Não foi possível comunicar com a API da Groq.\n\n"
                            + "Código do erro: "
                            + status;
        };
    }

    // =====================================================
    // CAUSA REAL
    // =====================================================

    private Throwable obterCausa(
            Throwable erro
    ) {

        Throwable atual =
                erro;

        while (
                (atual instanceof CompletionException
                        || atual instanceof RuntimeException)
                        && atual.getCause() != null
        ) {

            atual =
                    atual.getCause();
        }

        return atual;
    }

    // =====================================================
    // ERROS AMIGÁVEIS
    // =====================================================

    private RuntimeException criarErroAmigavel(
            Throwable erro
    ) {

        if (erro == null) {

            return new RuntimeException(
                    "❌ Ocorreu um erro desconhecido."
            );
        }

        if (erro instanceof GroqApiException) {

            return (GroqApiException) erro;
        }

        if (erro instanceof UnknownHostException) {

            return new RuntimeException(
                    "🌐 Não foi possível acessar a internet.\n\n"
                            + "Verifique sua conexão e tente novamente."
            );
        }

        if (erro instanceof ConnectException) {

            return new RuntimeException(
                    "📡 Não foi possível conectar ao servidor.\n\n"
                            + "Verifique sua conexão com a internet."
            );
        }

        if (erro instanceof HttpTimeoutException) {

            return new RuntimeException(
                    "⏱️ A pesquisa demorou muito para responder.\n\n"
                            + "Tente novamente."
            );
        }

        if (erro instanceof IOException) {

            return new RuntimeException(
                    "📡 Ocorreu uma falha de comunicação.\n\n"
                            + "Verifique sua internet e tente novamente."
            );
        }

        if (erro instanceof IllegalStateException
                && erro.getMessage() != null
                && erro.getMessage().contains(
                "GROQ_API_KEY"
        )) {

            return new RuntimeException(
                    "🔑 A chave da API da Groq não foi configurada.\n\n"
                            + "Configure GROQ_API_KEY e reinicie a aplicação."
            );
        }

        return new RuntimeException(
                "❌ Ocorreu um erro ao comunicar com a IA.\n\n"
                        + "Tente novamente."
        );
    }

    // =====================================================
    // EXCEÇÃO GROQ
    // =====================================================

    private static class GroqApiException
            extends RuntimeException {

        private final int statusCode;

        public GroqApiException(
                int statusCode,
                String message
        ) {

            super(message);

            this.statusCode =
                    statusCode;
        }

        public int getStatusCode() {

            return statusCode;
        }
    }
}