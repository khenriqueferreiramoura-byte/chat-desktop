package com.example.chatdesktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main extends Application {

    private TextArea chatArea;
    private TextField mensagemField;
    private Button enviarButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /*
     * COLOQUE AQUI SUA NOVA CHAVE DA GROQ.
     *
     * Não compartilhe essa chave e não publique este código
     * no GitHub enquanto ela estiver aqui.
     */
    private final String API_KEY = "COLOQUE_SUA_CHAVE_AQUI";

    /*
     * Modelo utilizado pela Groq.
     */
    private final String MODEL = "openai/gpt-oss-20b";

    @Override
    public void start(Stage stage) {

        // ==============================
        // ÁREA DO CHAT
        // ==============================

        chatArea = new TextArea();

        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        chatArea.setText(
                "Chat iniciado!\n\n" +
                        "Olá! Sou seu assistente.\n" +
                        "Digite uma mensagem abaixo.\n\n"
        );

        // ==============================
        // CAMPO DE MENSAGEM
        // ==============================

        mensagemField = new TextField();

        mensagemField.setPromptText(
                "Digite sua mensagem..."
        );

        // ==============================
        // BOTÃO ENVIAR
        // ==============================

        enviarButton = new Button("Enviar");

        enviarButton.setOnAction(
                event -> enviarMensagem()
        );

        // ENTER também envia
        mensagemField.setOnAction(
                event -> enviarMensagem()
        );

        // ==============================
        // ÁREA INFERIOR
        // ==============================

        HBox entrada = new HBox(10);

        entrada.setPadding(
                new Insets(10)
        );

        entrada.getChildren().addAll(
                mensagemField,
                enviarButton
        );

        HBox.setHgrow(
                mensagemField,
                Priority.ALWAYS
        );

        // ==============================
        // LAYOUT PRINCIPAL
        // ==============================

        BorderPane root = new BorderPane();

        root.setCenter(chatArea);
        root.setBottom(entrada);

        BorderPane.setMargin(
                chatArea,
                new Insets(10)
        );

        // ==============================
        // JANELA
        // ==============================

        Scene scene = new Scene(
                root,
                700,
                500
        );

        stage.setTitle("Chat com Groq");

        stage.setScene(scene);

        stage.show();

        mensagemField.requestFocus();
    }

    // ==================================================
    // ENVIAR MENSAGEM
    // ==================================================

    private void enviarMensagem() {

        String mensagem =
                mensagemField
                        .getText()
                        .trim();

        if (mensagem.isEmpty()) {
            return;
        }

        // Mostra mensagem do usuário
        chatArea.appendText(
                "Você: " + mensagem + "\n\n"
        );

        mensagemField.clear();

        enviarButton.setDisable(true);

        // ==================================================
        // THREAD PARA NÃO TRAVAR O JAVA FX
        // ==================================================

        Thread thread = new Thread(() -> {

            try {

                String resposta =
                        chamarGroq(mensagem);

                Platform.runLater(() -> {

                    chatArea.appendText(
                            "IA: " + resposta + "\n\n"
                    );

                    enviarButton.setDisable(false);

                    mensagemField.requestFocus();
                });

            } catch (Exception e) {

                Platform.runLater(() -> {

                    chatArea.appendText(
                            "Erro:\n"
                                    + e.getMessage()
                                    + "\n\n"
                    );

                    enviarButton.setDisable(false);

                    mensagemField.requestFocus();
                });
            }

        });

        thread.setDaemon(true);

        thread.start();
    }

    // ==================================================
    // CHAMADA PARA A GROQ
    // ==================================================

    private String chamarGroq(String mensagem)
            throws Exception {

        // Verifica se você realmente colocou a chave
        if (API_KEY.equals("COLOQUE_SUA_CHAVE_AQUI")
                || API_KEY.isBlank()) {

            throw new RuntimeException(
                    "Coloque sua chave da Groq na variável API_KEY."
            );
        }

        // ==================================================
        // JSON DA REQUISIÇÃO
        // ==================================================

        String json = """
                {
                    "model": "%s",
                    "messages": [
                        {
                            "role": "system",
                            "content": "Você é um assistente útil, educado e objetivo. Responda em português do Brasil."
                        },
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ],
                    "temperature": 1,
                    "max_completion_tokens": 1024
                }
                """.formatted(
                MODEL,
                escaparJson(mensagem)
        );

        // ==================================================
        // REQUISIÇÃO HTTP
        // ==================================================

        HttpRequest request =
                HttpRequest.newBuilder()

                        .uri(
                                URI.create(
                                        "https://api.groq.com/openai/v1/chat/completions"
                                )
                        )

                        .header(
                                "Authorization",
                                "Bearer " + API_KEY
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

        // ==================================================
        // ENVIA PARA A GROQ
        // ==================================================

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        // ==================================================
        // VERIFICA RESPOSTA
        // ==================================================

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "Erro da API Groq.\n\n"
                            + "HTTP: "
                            + response.statusCode()
                            + "\n\n"
                            + response.body()
            );
        }

        // ==================================================
        // EXTRAI RESPOSTA
        // ==================================================

        return extrairResposta(
                response.body()
        );
    }

    // ==================================================
    // ESCAPAR TEXTO PARA JSON
    // ==================================================

    private String escaparJson(String texto) {

        return texto
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ==================================================
    // EXTRAIR RESPOSTA DA GROQ
    // ==================================================

    private String extrairResposta(String json) {

        String marcador =
                "\"content\":\"";

        int inicio =
                json.indexOf(marcador);

        if (inicio == -1) {

            return "Não consegui interpretar a resposta da IA.\n\n"
                    + json;
        }

        inicio += marcador.length();

        StringBuilder resposta =
                new StringBuilder();

        boolean escapado = false;

        for (int i = inicio;
             i < json.length();
             i++) {

            char caractere =
                    json.charAt(i);

            // ------------------------------------------
            // Caractere escapado
            // ------------------------------------------

            if (escapado) {

                switch (caractere) {

                    case 'n':
                        resposta.append('\n');
                        break;

                    case 'r':
                        resposta.append('\r');
                        break;

                    case 't':
                        resposta.append('\t');
                        break;

                    case '"':
                        resposta.append('"');
                        break;

                    case '\\':
                        resposta.append('\\');
                        break;

                    default:
                        resposta.append(caractere);
                        break;
                }

                escapado = false;

            }

            // ------------------------------------------
            // Início de escape
            // ------------------------------------------

            else if (caractere == '\\') {

                escapado = true;

            }

            // ------------------------------------------
            // Fim do conteúdo
            // ------------------------------------------

            else if (caractere == '"') {

                break;

            }

            // ------------------------------------------
            // Texto normal
            // ------------------------------------------

            else {

                resposta.append(caractere);
            }
        }

        return resposta.toString();
    }

    // ==================================================
    // MAIN
    // ==================================================

    public static void main(String[] args) {

        launch(args);
    }
}