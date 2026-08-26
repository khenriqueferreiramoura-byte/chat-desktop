package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.DocumentService;
import com.example.chatdesktop.service.GroqService;
import com.example.chatdesktop.service.RagService;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    @FXML
    private VBox sidebar;

    @FXML
    private ListView<String> listaHistorico;

    @FXML
    private Button botaoOpcoes;

    @FXML
    private Button botaoTema;


    // =====================================================
    // SERVIÇOS
    // =====================================================

    private final GroqService groq =
            new GroqService();


    private final RagService rag =
            new RagService(
                    new DocumentService(
                            Path.of("knowledge")
                    )
            );


    // =====================================================
    // HISTÓRICO ATUAL
    // =====================================================

    private final List<ChatMessage> historico =
            new ArrayList<>();


    // =====================================================
    // CONVERSAS
    // =====================================================

    private final List<List<ChatMessage>> conversas =
            new ArrayList<>();


    private final ObservableList<String> listaConversas =
            FXCollections.observableArrayList();


    // =====================================================
    // CONTROLE
    // =====================================================

    private int idConversa = 0;

    private boolean temaEscuro = true;

    private boolean tituloGerado = false;

    private boolean gerandoTitulo = false;


    private String tituloConversaAtual =
            "Nova conversa";


    // =====================================================
    // PROMPT RAG
    // =====================================================

    private static final String PROMPT_RAG =
            """
            Você é a Orbit-IA, um assistente baseado em documentos.

            Sua função é responder perguntas utilizando exclusivamente
            as informações presentes nos documentos fornecidos no contexto.

            REGRAS OBRIGATÓRIAS:

            1. Utilize somente informações presentes no contexto.
            2. Não utilize conhecimento externo.
            3. Não invente informações.
            4. Não faça suposições.
            5. Não complete informações ausentes com conhecimento próprio.
            6. Não contradiga os documentos.
            7. Responda em português do Brasil.
            8. Seja objetivo e claro.
            9. Caso a informação solicitada não esteja no contexto,
               responda:

               "Não encontrei essa informação nos documentos disponíveis."

            A resposta deve ser fundamentada exclusivamente
            nos documentos recuperados pelo sistema RAG.
            """;


    // =====================================================
    // PROMPT TÍTULO
    // =====================================================

    private static final String PROMPT_TITULO =
            """
            Você é responsável por criar títulos para conversas.

            Analise a primeira mensagem do usuário e crie um título
            curto que represente o assunto principal.

            Regras:

            - Português do Brasil.
            - Máximo de 6 palavras.
            - Não use aspas.
            - Não use ponto final.
            - Responda somente com o título.
            """;


    // =====================================================
    // CSS
    // =====================================================

    private static final String CLARO =
            "/css/tema-claro.css";

    private static final String ESCURO =
            "/css/tema-escuro.css";


    // =====================================================
    // INITIALIZE
    // =====================================================

    @FXML
    private void initialize() {

        iniciarHistorico();


        listaHistorico.setItems(
                listaConversas
        );


        listaHistorico.setOnMouseClicked(e -> {

            if (e.getClickCount() == 2) {

                int indice =
                        listaHistorico
                                .getSelectionModel()
                                .getSelectedIndex();

                if (indice >= 0) {

                    carregarConversa(
                            indice
                    );
                }
            }
        });


        adicionarIA(
                "🍥 Seja bem-vindo à Orbit-IA!\n\n"
                        + "Como posso ajudar você?"
        );


        Platform.runLater(() -> {

            campoMensagem.requestFocus();

            configurarAtalhos();
        });
    }


    // =====================================================
    // INICIAR HISTÓRICO
    // =====================================================

    private void iniciarHistorico() {

        historico.clear();


        historico.add(
                new ChatMessage(
                        "system",
                        PROMPT_RAG
                )
        );


        tituloGerado = false;

        gerandoTitulo = false;
    }


    // =====================================================
    // ENVIAR
    // =====================================================

    @FXML
    private void enviarMensagem() {

        String texto =
                campoMensagem
                        .getText()
                        .trim();


        if (texto.isEmpty() ||
                campoMensagem.isDisabled()) {

            return;
        }


        campoMensagem.clear();


        // =================================================
        // PRIMEIRA MENSAGEM
        // =================================================

        boolean primeiraMensagem =
                historico.stream()
                        .noneMatch(
                                m ->
                                        m.getRole()
                                                .equals("user")
                        );


        // =================================================
        // UI
        // =================================================

        adicionarUsuario(texto);


        // =================================================
        // HISTÓRICO
        // =================================================

        historico.add(
                new ChatMessage(
                        "user",
                        texto
                )
        );


        // =================================================
        // ID
        // =================================================

        int id =
                idConversa;


        // =================================================
        // BLOQUEIA INPUT
        // =================================================

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);


        // =================================================
        // TÍTULO
        // =================================================

        if (primeiraMensagem &&
                !tituloGerado) {

            gerarTituloAutomaticamente(
                    texto,
                    id
            );
        }


        // =================================================
        // RAG
        // =================================================

        CompletableFuture
                .supplyAsync(() ->
                        rag.criarContexto(
                                texto,
                                5
                        )
                )


                // =================================================
                // MONTA PROMPT
                // =================================================

                .thenApply(contexto ->
                        criarMensagensParaIA(
                                texto,
                                contexto
                        )
                )


                // =================================================
                // GROQ
                // =================================================

                .thenCompose(
                        groq::enviarMensagem
                )


                // =================================================
                // RESPOSTA
                // =================================================

                .thenAccept(resposta -> {

                    if (id != idConversa) {
                        return;
                    }


                    historico.add(
                            new ChatMessage(
                                    "assistant",
                                    resposta
                            )
                    );


                    Platform.runLater(() -> {

                        adicionarIA(
                                resposta
                        );

                        liberar();
                    });
                })


                // =================================================
                // ERRO
                // =================================================

                .exceptionally(erro -> {

                    if (id != idConversa) {
                        return null;
                    }


                    Platform.runLater(() -> {

                        adicionarIA(
                                erroAmigavel(
                                        obterCausa(
                                                erro
                                        )
                                )
                        );

                        liberar();
                    });


                    return null;
                });
    }


    // =====================================================
    // MONTAR MENSAGENS RAG
    // =====================================================

    private List<ChatMessage> criarMensagensParaIA(
            String pergunta,
            String contexto
    ) {

        List<ChatMessage> mensagens =
                new ArrayList<>();


        mensagens.add(
                new ChatMessage(
                        "system",
                        PROMPT_RAG
                )
        );


        // =================================================
        // HISTÓRICO
        // =================================================

        for (ChatMessage mensagem : historico) {

            if (mensagem.getRole()
                    .equals("system")) {

                continue;
            }


            /*
             * A pergunta atual será adicionada
             * novamente com o contexto.
             */
            if (mensagem.getRole()
                    .equals("user")
                    &&
                    mensagem.getContent()
                            .equals(pergunta)) {

                continue;
            }


            mensagens.add(mensagem);
        }


        // =================================================
        // CONTEXTO
        // =================================================

        String contextoFinal;


        if (contexto == null ||
                contexto.isBlank()) {

            contextoFinal =
                    "NENHUM DOCUMENTO RELEVANTE FOI ENCONTRADO.";
        } else {

            contextoFinal =
                    contexto;
        }


        String mensagemFinal =
                """
                CONTEXTO RECUPERADO DOS DOCUMENTOS
                ==================================

                %s


                PERGUNTA DO USUÁRIO
                ===================

                %s


                INSTRUÇÕES
                ==========

                Responda utilizando SOMENTE as informações
                presentes no contexto acima.

                Se a resposta não estiver presente no contexto,
                responda:

                "Não encontrei essa informação nos documentos disponíveis."

                Não utilize conhecimento externo.
                Não faça suposições.
                Não invente informações.
                """.formatted(
                        contextoFinal,
                        pergunta
                );


        mensagens.add(
                new ChatMessage(
                        "user",
                        mensagemFinal
                )
        );


        return mensagens;
    }


    // =====================================================
    // TÍTULO
    // =====================================================

    private void gerarTituloAutomaticamente(
            String primeiraMensagem,
            int id
    ) {

        if (gerandoTitulo) {
            return;
        }


        gerandoTitulo = true;


        List<ChatMessage> mensagens =
                new ArrayList<>();


        mensagens.add(
                new ChatMessage(
                        "system",
                        PROMPT_TITULO
                )
        );


        mensagens.add(
                new ChatMessage(
                        "user",
                        primeiraMensagem
                )
        );


        groq.enviarMensagem(
                        mensagens
                )


                .thenAccept(titulo -> {

                    if (id != idConversa) {
                        return;
                    }


                    String tituloFinal =
                            limparTitulo(
                                    titulo
                            );


                    if (tituloFinal.isBlank()) {

                        tituloFinal =
                                criarTituloFallback(
                                        primeiraMensagem
                                );
                    }


                    final String tituloPronto =
                            tituloFinal;


                    Platform.runLater(() -> {

                        tituloGerado = true;

                        gerandoTitulo = false;

                        atualizarTituloConversaAtual(
                                tituloPronto
                        );
                    });
                })


                .exceptionally(erro -> {

                    Platform.runLater(() -> {

                        gerandoTitulo = false;

                        tituloGerado = true;


                        atualizarTituloConversaAtual(
                                criarTituloFallback(
                                        primeiraMensagem
                                )
                        );
                    });


                    return null;
                });
    }


    // =====================================================
    // LIMPAR TÍTULO
    // =====================================================

    private String limparTitulo(
            String titulo
    ) {

        if (titulo == null) {
            return "";
        }


        titulo =
                titulo
                        .replace("\"", "")
                        .replace("'", "")
                        .replace("\n", " ")
                        .replace("\r", " ")
                        .trim();


        if (titulo.length() > 40) {

            titulo =
                    titulo.substring(
                                    0,
                                    40
                            )
                            .trim();


            int ultimoEspaco =
                    titulo.lastIndexOf(
                            " "
                    );


            if (ultimoEspaco > 15) {

                titulo =
                        titulo.substring(
                                0,
                                ultimoEspaco
                        );
            }
        }


        return titulo;
    }


    // =====================================================
    // FALLBACK
    // =====================================================

    private String criarTituloFallback(
            String mensagem
    ) {

        if (mensagem == null ||
                mensagem.isBlank()) {

            return "Nova conversa";
        }


        String titulo =
                mensagem
                        .replace(
                                "\n",
                                " "
                        )
                        .trim();


        if (titulo.length() > 30) {

            titulo =
                    titulo.substring(
                                    0,
                                    30
                            )
                            .trim();


            int ultimoEspaco =
                    titulo.lastIndexOf(
                            " "
                    );


            if (ultimoEspaco > 10) {

                titulo =
                        titulo.substring(
                                0,
                                ultimoEspaco
                        );
            }


            titulo += "...";
        }


        return titulo;
    }


    // =====================================================
    // ATUALIZAR TÍTULO
    // =====================================================

    private void atualizarTituloConversaAtual(
            String titulo
    ) {

        tituloConversaAtual =
                titulo;
    }


    // =====================================================
    // USUÁRIO
    // =====================================================

    private void adicionarUsuario(
            String texto
    ) {

        Label label =
                new Label(
                        texto
                );


        label.setWrapText(true);

        label.setMaxWidth(550);


        label.getStyleClass()
                .add(
                        "user-message"
                );


        Label usuario =
                new Label("👤");


        HBox linha =
                new HBox(
                        10,
                        usuario,
                        label
                );


        linha.setAlignment(
                Pos.CENTER_RIGHT
        );


        chatContainer
                .getChildren()
                .add(linha);


        scroll();
    }


    // =====================================================
    // IA
    // =====================================================

    private void adicionarIA(
            String texto
    ) {

        Label label =
                new Label(
                        texto
                );


        label.setWrapText(true);

        label.setMaxWidth(550);


        label.getStyleClass()
                .add(
                        "ai-message"
                );


        Label robo =
                new Label("🤖");


        HBox linha =
                new HBox(
                        10,
                        label,
                        robo
                );


        linha.setAlignment(
                Pos.CENTER_LEFT
        );


        Button copiar =
                new Button(
                        "📋 Copiar"
                );


        copiar.getStyleClass()
                .add(
                        "copy-button"
                );


        copiar.setOnAction(e -> {

            ClipboardContent content =
                    new ClipboardContent();


            content.putString(
                    texto
            );


            Clipboard
                    .getSystemClipboard()
                    .setContent(
                            content
                    );


            copiar.setText(
                    "✓ Copiado"
            );


            PauseTransition pause =
                    new PauseTransition(
                            Duration.seconds(
                                    1.5
                            )
                    );


            pause.setOnFinished(
                    x ->
                            copiar.setText(
                                    "📋 Copiar"
                            )
            );


            pause.play();
        });


        VBox bloco =
                new VBox(
                        5,
                        linha,
                        copiar
                );


        bloco.setAlignment(
                Pos.CENTER_LEFT
        );


        chatContainer
                .getChildren()
                .add(bloco);


        scroll();
    }


    // =====================================================
    // NOVA CONVERSA
    // =====================================================

    @FXML
    private void novaConversa() {

        boolean possuiUsuario =
                historico.stream()
                        .anyMatch(
                                m ->
                                        m.getRole()
                                                .equals("user")
                        );


        if (possuiUsuario) {

            conversas.add(
                    0,
                    new ArrayList<>(
                            historico
                    )
            );


            String titulo =
                    tituloConversaAtual;


            if (titulo == null ||
                    titulo.isBlank()) {

                titulo =
                        "Nova conversa";
            }


            listaConversas.add(
                    0,
                    titulo
            );
        }


        idConversa++;


        iniciarHistorico();


        tituloConversaAtual =
                "Nova conversa";


        chatContainer
                .getChildren()
                .clear();


        adicionarIA(
                "🍥 Nova conversa iniciada!\n\n"
                        + "Como posso ajudar?"
        );


        liberar();
    }


    // =====================================================
    // CARREGAR CONVERSA
    // =====================================================

    private void carregarConversa(
            int indice
    ) {

        if (indice < 0 ||
                indice >= conversas.size()) {

            return;
        }


        idConversa++;


        historico.clear();


        historico.addAll(
                conversas.get(
                        indice
                )
        );


        chatContainer
                .getChildren()
                .clear();


        for (ChatMessage mensagem :
                historico) {

            if (mensagem.getRole()
                    .equals("user")) {

                adicionarUsuario(
                        mensagem.getContent()
                );
            }


            else if (
                    mensagem.getRole()
                            .equals("assistant")
            ) {

                adicionarIA(
                        mensagem.getContent()
                );
            }
        }


        if (indice <
                listaConversas.size()) {

            tituloConversaAtual =
                    listaConversas.get(
                            indice
                    );
        }


        tituloGerado = true;

        gerandoTitulo = false;


        liberar();
    }


    // =====================================================
    // TEMA
    // =====================================================

    @FXML
    private void alternarTema() {

        Scene scene =
                chatContainer.getScene();


        if (scene == null) {
            return;
        }


        String antigo =
                temaEscuro
                        ? ESCURO
                        : CLARO;


        String novo =
                temaEscuro
                        ? CLARO
                        : ESCURO;


        java.net.URL urlAntigo =
                getClass()
                        .getResource(
                                antigo
                        );


        java.net.URL urlNovo =
                getClass()
                        .getResource(
                                novo
                        );


        if (urlAntigo == null ||
                urlNovo == null) {

            System.err.println(
                    "CSS do tema não encontrado."
            );

            return;
        }


        scene.getStylesheets()
                .remove(
                        urlAntigo
                                .toExternalForm()
                );


        scene.getStylesheets()
                .add(
                        urlNovo
                                .toExternalForm()
                );


        temaEscuro =
                !temaEscuro;


        botaoTema.setText(
                temaEscuro
                        ? "☀"
                        : "🌙"
        );
    }


    // =====================================================
    // OPÇÕES
    // =====================================================

    @FXML
    private void abrirOpcoes() {

        new Alert(
                Alert.AlertType.INFORMATION,
                """
                ☀ / 🌙  Alterar tema

                Enter → Enviar

                Ctrl + N → Nova conversa

                Ctrl + L → Limpar campo
                """
        ).showAndWait();
    }


    // =====================================================
    // ATALHOS
    // =====================================================

    private void configurarAtalhos() {

        Scene scene =
                chatContainer.getScene();


        if (scene == null) {
            return;
        }


        scene.addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> {

                    if (e.getCode()
                            == KeyCode.ENTER
                            &&
                            !e.isControlDown()
                            &&
                            !e.isShiftDown()) {

                        enviarMensagem();

                        e.consume();
                    }


                    else if (
                            e.isControlDown()
                                    &&
                                    e.getCode()
                                            == KeyCode.N
                    ) {

                        novaConversa();

                        e.consume();
                    }


                    else if (
                            e.isControlDown()
                                    &&
                                    e.getCode()
                                            == KeyCode.L
                    ) {

                        campoMensagem.clear();

                        campoMensagem.requestFocus();

                        e.consume();
                    }
                }
        );
    }


    // =====================================================
    // ERRO
    // =====================================================

    private String erroAmigavel(
            Throwable erro
    ) {

        String msg =
                erro.getMessage();


        if (msg == null) {

            return "❌ Erro desconhecido.";
        }


        msg =
                msg.toLowerCase();


        if (msg.contains("401")) {

            return "🔑 Chave da API inválida.";
        }


        if (msg.contains("429")) {

            return "⏳ Limite da API atingido. Tente novamente depois.";
        }


        if (msg.contains("timeout") ||
                msg.contains("connect") ||
                msg.contains("network")) {

            return "🌐 Verifique sua conexão com a internet.";
        }


        if (msg.contains("500") ||
                msg.contains("502") ||
                msg.contains("503")) {

            return "⚠️ Serviço temporariamente indisponível.";
        }


        if (msg.contains("knowledge") ||
                msg.contains("rag")) {

            return "📚 Não foi possível consultar a base de conhecimento.";
        }


        return "❌ Não foi possível obter uma resposta da IA.";
    }


    // =====================================================
    // CAUSA DO ERRO
    // =====================================================

    private Throwable obterCausa(
            Throwable erro
    ) {

        if (erro == null) {

            return new RuntimeException(
                    "Erro desconhecido."
            );
        }


        if (erro.getCause() != null) {

            return erro.getCause();
        }


        return erro;
    }


    // =====================================================
    // SCROLL
    // =====================================================

    private void scroll() {

        Platform.runLater(() ->
                chatScroll.setVvalue(
                        1
                )
        );
    }


    // =====================================================
    // LIBERAR INPUT
    // =====================================================

    private void liberar() {

        campoMensagem.setDisable(
                false
        );

        botaoEnviar.setDisable(
                false
        );

        campoMensagem.requestFocus();
    }
}