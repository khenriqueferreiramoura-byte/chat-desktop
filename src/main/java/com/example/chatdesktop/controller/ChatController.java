package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.GroqService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private VBox chatContainer;

    @FXML
    private ScrollPane chatScroll;

    @FXML
    private TextField campoMensagem;

    @FXML
    private Button botaoEnviar;

    @FXML
    private Button botaoNovaConversa;


    private final GroqService groqService;

    private final List<ChatMessage> historico;


    private int idConversa = 0;


    private static final String SYSTEM_PROMPT =
            "Você é um assistente útil, educado e objetivo. " +
                    "Responda sempre em português do Brasil.";


    public ChatController() {

        this.groqService = new GroqService();

        this.historico = new ArrayList<>();
    }


    @FXML
    private void initialize() {

        iniciarHistorico();

        adicionarMensagemIA(
                "🍥 Seja bem-vindo ao Naruto AI!\n\n" +
                        "Estou pronto para conversar com você. " +
                        "Faça uma pergunta, peça uma explicação " +
                        "ou comece uma nova conversa."
        );

        campoMensagem.requestFocus();
    }


    /*
     * =====================================================
     * INICIAR HISTÓRICO
     * =====================================================
     */

    private void iniciarHistorico() {

        historico.clear();

        historico.add(
                new ChatMessage(
                        "system",
                        SYSTEM_PROMPT
                )
        );
    }


    /*
     * =====================================================
     * ENVIAR MENSAGEM
     * =====================================================
     */

    @FXML
    private void enviarMensagem() {

        String mensagem =
                campoMensagem
                        .getText()
                        .trim();


        if (mensagem.isEmpty()) {
            return;
        }


        campoMensagem.clear();


        /*
         * Mostra a mensagem do usuário
         */

        adicionarMensagemUsuario(mensagem);


        /*
         * Adiciona ao histórico
         */

        historico.add(
                new ChatMessage(
                        "user",
                        mensagem
                )
        );


        int conversaAtual = idConversa;


        bloquearInterface();


        groqService
                .enviarMensagem(historico)
                .thenAccept(resposta ->
                        receberResposta(
                                resposta,
                                conversaAtual
                        )
                )
                .exceptionally(erro ->
                        tratarErro(
                                erro,
                                conversaAtual
                        )
                );
    }


    /*
     * =====================================================
     * MENSAGEM DO USUÁRIO
     * =====================================================
     */

    private void adicionarMensagemUsuario(
            String mensagem
    ) {

        /*
         * Container da mensagem
         */

        HBox linha = new HBox();

        linha.setAlignment(Pos.CENTER_RIGHT);

        linha.setSpacing(10);


        /*
         * ÍCONE DO USUÁRIO
         */

        Label iconeUsuario =
                new Label("👤");

        iconeUsuario.getStyleClass()
                .add("user-icon");


        /*
         * TEXTO
         */

        Label texto =
                new Label(mensagem);

        texto.setWrapText(true);

        texto.setMaxWidth(550);

        texto.getStyleClass()
                .add("user-message");


        /*
         * Ícone fica à esquerda
         */

        linha.getChildren().addAll(
                iconeUsuario,
                texto
        );


        chatContainer
                .getChildren()
                .add(linha);


        rolarParaBaixo();
    }


    /*
     * =====================================================
     * MENSAGEM DA IA
     * =====================================================
     */

    private void adicionarMensagemIA(String mensagem) {

        VBox blocoMensagem = new VBox(5);
        blocoMensagem.setAlignment(Pos.CENTER_RIGHT);

        // Linha da mensagem
        HBox linhaMensagem = new HBox(10);
        linhaMensagem.setAlignment(Pos.CENTER_RIGHT);

        // Texto da IA
        Label texto = new Label(mensagem);
        texto.setWrapText(true);
        texto.setMaxWidth(550);
        texto.getStyleClass().add("ai-message");

        // Ícone da IA
        Label iconeIA = new Label("🤖");
        iconeIA.getStyleClass().add("ai-icon");

        linhaMensagem.getChildren().addAll(
                texto,
                iconeIA
        );

        // Botão copiar
        Button botaoCopiar = new Button("📋 Copiar");

        botaoCopiar.getStyleClass().add("copy-button");

        botaoCopiar.setOnAction(event -> {

            Clipboard clipboard =
                    Clipboard.getSystemClipboard();

            ClipboardContent content =
                    new ClipboardContent();

            content.putString(mensagem);

            clipboard.setContent(content);

            // Feedback visual
            botaoCopiar.setText("✓ Copiado");

            javafx.animation.PauseTransition pausa =
                    new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(1.5)
                    );

            pausa.setOnFinished(e ->
                    botaoCopiar.setText("📋 Copiar")
            );

            pausa.play();
        });

        // Adiciona mensagem + botão
        blocoMensagem.getChildren().addAll(
                linhaMensagem,
                botaoCopiar
        );

        chatContainer.getChildren().add(
                blocoMensagem
        );

        rolarParaBaixo();
    }


    /*
     * =====================================================
     * RECEBER RESPOSTA
     * =====================================================
     */

    private void receberResposta(
            String resposta,
            int conversaDaRequisicao
    ) {

        /*
         * Ignora resposta de uma conversa antiga.
         */

        if (conversaDaRequisicao != idConversa) {
            return;
        }


        historico.add(
                new ChatMessage(
                        "assistant",
                        resposta
                )
        );


        Platform.runLater(() -> {

            adicionarMensagemIA(resposta);

            liberarInterface();
        });
    }


    /*
     * =====================================================
     * ERRO
     * =====================================================
     */

    private Void tratarErro(
            Throwable erro,
            int conversaDaRequisicao
    ) {

        if (conversaDaRequisicao != idConversa) {
            return null;
        }


        Throwable causa =
                erro.getCause() != null
                        ? erro.getCause()
                        : erro;


        Platform.runLater(() -> {

            adicionarMensagemIA(
                    "❌ Ocorreu um erro:\n\n"
                            + causa.getMessage()
            );

            liberarInterface();
        });


        return null;
    }


    /*
     * =====================================================
     * NOVA CONVERSA
     * =====================================================
     */

    @FXML
    private void novaConversa() {

        idConversa++;


        /*
         * Limpa o histórico
         */

        iniciarHistorico();


        /*
         * Limpa a interface
         */

        chatContainer
                .getChildren()
                .clear();


        /*
         * Mensagem inicial da nova conversa
         */

        adicionarMensagemIA(
                "🍥 Nova conversa iniciada!\n\n" +
                        "Como posso ajudar você?"
        );


        campoMensagem.clear();


        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);


        campoMensagem.requestFocus();
    }


    /*
     * =====================================================
     * SCROLL AUTOMÁTICO
     * =====================================================
     */

    private void rolarParaBaixo() {

        Platform.runLater(() ->
                chatScroll.setVvalue(1.0)
        );
    }


    /*
     * =====================================================
     * BLOQUEAR
     * =====================================================
     */

    private void bloquearInterface() {

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);

        /*
         * Nova conversa continua disponível.
         */

        botaoNovaConversa.setDisable(false);
    }


    /*
     * =====================================================
     * LIBERAR
     * =====================================================
     */

    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);

        campoMensagem.requestFocus();
    }

}