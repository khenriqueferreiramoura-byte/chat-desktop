package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.GroqService;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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

    private final GroqService groq = new GroqService();


    // =====================================================
    // CONVERSA ATUAL
    // =====================================================

    private final List<ChatMessage> historico = new ArrayList<>();


    // =====================================================
    // CONVERSAS SALVAS
    // =====================================================

    private final List<List<ChatMessage>> conversas =
            new ArrayList<>();


    // =====================================================
    // TÍTULOS
    // =====================================================

    private final ObservableList<String> listaConversas =
            FXCollections.observableArrayList();


    // =====================================================
    // CONTROLE DA CONVERSA
    // =====================================================

    private int idConversa = 0;

    private boolean temaEscuro = true;

    private boolean tituloGerado = false;

    private boolean gerandoTitulo = false;


    // =====================================================
    // PROMPT PRINCIPAL
    // =====================================================

    private static final String PROMPT =
            "Você é um assistente útil, educado e objetivo. " +
                    "Responda sempre em português do Brasil.";


    // =====================================================
    // PROMPT PARA TÍTULO
    // =====================================================

    private static final String PROMPT_TITULO =
            "Você é responsável por criar títulos para conversas. " +
                    "Analise a primeira mensagem do usuário e crie um título " +
                    "curto que represente o assunto principal da conversa. " +
                    "O título DEVE estar obrigatoriamente em português do Brasil. " +
                    "Use palavras naturais e comuns em português. " +
                    "Nunca responda em inglês ou em outro idioma. " +
                    "O título deve ter no máximo 6 palavras. " +
                    "Não use aspas. " +
                    "Não coloque ponto final. " +
                    "Responda somente com o título em português.";


    // =====================================================
    // CSS
    // =====================================================

    private static final String CLARO =
            "/css/tema-claro.css";

    private static final String ESCURO =
            "/css/tema-escuro.css";


    // =====================================================
    // INICIALIZAÇÃO
    // =====================================================

    @FXML
    private void initialize() {

        iniciarHistorico();

        listaHistorico.setItems(listaConversas);


        // ---------------------------------------------
        // Abrir conversa com duplo clique
        // ---------------------------------------------

        listaHistorico.setOnMouseClicked(e -> {

            if (e.getClickCount() == 2) {

                int i = listaHistorico
                        .getSelectionModel()
                        .getSelectedIndex();

                if (i >= 0) {
                    carregarConversa(i);
                }
            }
        });


        // ---------------------------------------------
        // Mensagem inicial
        // ---------------------------------------------

        adicionarIA(
                "🍥 Seja bem-vindo à Orbit-IA!\n\n" +
                        "Como posso ajudar você?"
        );


        // ---------------------------------------------
        // Foco inicial
        // ---------------------------------------------

        Platform.runLater(() -> {

            campoMensagem.requestFocus();

            configurarAtalhos();
        });
    }


    // =====================================================
    // HISTÓRICO
    // =====================================================

    private void iniciarHistorico() {

        historico.clear();

        historico.add(
                new ChatMessage(
                        "system",
                        PROMPT
                )
        );

        tituloGerado = false;

        gerandoTitulo = false;
    }


    // =====================================================
    // ENVIAR MENSAGEM
    // =====================================================

    @FXML
    private void enviarMensagem() {

        String texto = campoMensagem
                .getText()
                .trim();


        if (texto.isEmpty() ||
                campoMensagem.isDisabled()) {

            return;
        }


        campoMensagem.clear();


        // ---------------------------------------------
        // Verifica se é a primeira mensagem
        // ---------------------------------------------

        boolean primeiraMensagem =
                historico.stream()
                        .noneMatch(m ->
                                m.getRole().equals("user"));


        // ---------------------------------------------
        // Mostra mensagem do usuário
        // ---------------------------------------------

        adicionarUsuario(texto);


        // ---------------------------------------------
        // Adiciona ao histórico principal
        // ---------------------------------------------

        historico.add(
                new ChatMessage(
                        "user",
                        texto
                )
        );


        // ---------------------------------------------
        // Identificador da conversa
        // ---------------------------------------------

        int id = idConversa;


        // ---------------------------------------------
        // Desabilita entrada
        // ---------------------------------------------

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);


        // =================================================
        // GERA TÍTULO AUTOMATICAMENTE
        // =================================================

        if (primeiraMensagem && !tituloGerado) {

            gerarTituloAutomaticamente(
                    texto,
                    id
            );
        }


        // =================================================
        // ENVIA MENSAGEM NORMAL PARA A IA
        // =================================================

        groq.enviarMensagem(historico)

                .thenAccept(resposta -> {

                    // A conversa mudou enquanto a IA respondia
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

                        adicionarIA(resposta);

                        liberar();
                    });
                })


                .exceptionally(erro -> {

                    Platform.runLater(() -> {

                        adicionarIA(
                                erroAmigavel(erro)
                        );

                        liberar();
                    });

                    return null;
                });
    }


    // =====================================================
    // GERAR TÍTULO AUTOMATICAMENTE
    // =====================================================

    private void gerarTituloAutomaticamente(
            String primeiraMensagem,
            int id
    ) {

        if (gerandoTitulo) {
            return;
        }

        gerandoTitulo = true;

        List<ChatMessage> mensagensTitulo =
                new ArrayList<>();

        mensagensTitulo.add(
                new ChatMessage(
                        "system",
                        PROMPT_TITULO
                )
        );

        mensagensTitulo.add(
                new ChatMessage(
                        "user",
                        primeiraMensagem
                )
        );

        groq.enviarMensagem(mensagensTitulo)

                .thenAccept(respostaTitulo -> {

                    if (id != idConversa) {
                        return;
                    }

                    String tituloProcessado =
                            limparTitulo(respostaTitulo);

                    if (tituloProcessado.isBlank()) {

                        tituloProcessado =
                                criarTituloFallback(
                                        primeiraMensagem
                                );
                    }

                    // IMPORTANTE:
                    // depois que definimos o valor,
                    // criamos uma variável final.
                    final String tituloFinal =
                            tituloProcessado;

                    Platform.runLater(() -> {

                        tituloGerado = true;

                        gerandoTitulo = false;

                        atualizarTituloConversaAtual(
                                tituloFinal
                        );
                    });
                })

                .exceptionally(erro -> {

                    Platform.runLater(() -> {

                        gerandoTitulo = false;

                        tituloGerado = true;

                        final String tituloFallback =
                                criarTituloFallback(
                                        primeiraMensagem
                                );

                        atualizarTituloConversaAtual(
                                tituloFallback
                        );
                    });

                    return null;
                });
    }


    // =====================================================
    // LIMPAR TÍTULO
    // =====================================================

    private String limparTitulo(String titulo) {

        if (titulo == null) {
            return "";
        }


        titulo = titulo
                .replace("\"", "")
                .replace("'", "")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();


        /*
         * Evita títulos muito grandes.
         */

        if (titulo.length() > 40) {

            titulo =
                    titulo.substring(0, 40)
                            .trim();

            int ultimoEspaco =
                    titulo.lastIndexOf(" ");

            if (ultimoEspaco > 15) {

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
    // TÍTULO DE EMERGÊNCIA
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
                        .replace("\n", " ")
                        .trim();


        if (titulo.length() > 30) {

            titulo =
                    titulo.substring(0, 30)
                            .trim();

            int ultimoEspaco =
                    titulo.lastIndexOf(" ");

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
    // ATUALIZAR TÍTULO DA CONVERSA ATUAL
    // =====================================================

    private void atualizarTituloConversaAtual(
            String titulo
    ) {

        /*
         * A conversa ainda não foi salva.
         *
         * Guardamos temporariamente o título
         * através da variável abaixo.
         */

        tituloConversaAtual = titulo;
    }


    // =====================================================
    // TÍTULO ATUAL
    // =====================================================

    private String tituloConversaAtual =
            "Nova conversa";


    // =====================================================
    // USUÁRIO
    // =====================================================

    private void adicionarUsuario(String texto) {

        Label label =
                new Label(texto);


        label.setWrapText(true);

        label.setMaxWidth(550);


        label.getStyleClass()
                .add("user-message");


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
    // IA + COPIAR
    // =====================================================

    private void adicionarIA(String texto) {

        Label label =
                new Label(texto);


        label.setWrapText(true);

        label.setMaxWidth(550);


        label.getStyleClass()
                .add("ai-message");


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


        // ---------------------------------------------
        // BOTÃO COPIAR
        // ---------------------------------------------

        Button copiar =
                new Button("📋 Copiar");


        copiar.getStyleClass()
                .add("copy-button");


        copiar.setOnAction(e -> {

            ClipboardContent c =
                    new ClipboardContent();


            c.putString(texto);


            Clipboard
                    .getSystemClipboard()
                    .setContent(c);


            copiar.setText("✓ Copiado");


            PauseTransition p =
                    new PauseTransition(
                            Duration.seconds(1.5)
                    );


            p.setOnFinished(x ->
                    copiar.setText("📋 Copiar")
            );


            p.play();
        });


        VBox bloco =
                new VBox(
                        5,
                        linha,
                        copiar
                );


        bloco.setAlignment(
                Pos.CENTER_RIGHT
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

        /*
         * Só salva uma conversa se ela possuir
         * pelo menos uma mensagem do usuário.
         */

        if (historico.stream()
                .anyMatch(m ->
                        m.getRole().equals("user"))) {


            /*
             * IMPORTANTE:
             *
             * A conversa e seu título são adicionados
             * na MESMA posição.
             *
             * Isso corrige o problema de índice
             * que existia anteriormente.
             */

            conversas.add(
                    0,
                    new ArrayList<>(historico)
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


        // ---------------------------------------------
        // Nova identificação
        // ---------------------------------------------

        idConversa++;


        // ---------------------------------------------
        // Reinicia histórico
        // ---------------------------------------------

        iniciarHistorico();


        tituloConversaAtual =
                "Nova conversa";


        chatContainer
                .getChildren()
                .clear();


        adicionarIA(
                "🍥 Nova conversa iniciada!\n\n" +
                        "Como posso ajudar?"
        );


        liberar();
    }


    // =====================================================
    // CARREGAR CONVERSA
    // =====================================================

    private void carregarConversa(int i) {

        if (i < 0 ||
                i >= conversas.size()) {

            return;
        }


        idConversa++;


        historico.clear();


        historico.addAll(
                conversas.get(i)
        );


        chatContainer
                .getChildren()
                .clear();


        for (ChatMessage m : historico) {

            if (m.getRole()
                    .equals("user")) {

                adicionarUsuario(
                        m.getContent()
                );
            }

            else if (
                    m.getRole()
                            .equals("assistant")) {

                adicionarIA(
                        m.getContent()
                );
            }
        }


        /*
         * Recupera o título que está no
         * mesmo índice da conversa.
         */

        if (i < listaConversas.size()) {

            tituloConversaAtual =
                    listaConversas.get(i);
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
                getClass().getResource(antigo);


        java.net.URL urlNovo =
                getClass().getResource(novo);


        if (urlAntigo == null ||
                urlNovo == null) {

            System.err.println(
                    "CSS do tema não encontrado."
            );

            return;
        }


        scene.getStylesheets()
                .remove(
                        urlAntigo.toExternalForm()
                );


        scene.getStylesheets()
                .add(
                        urlNovo.toExternalForm()
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
                "☀ / 🌙  Alterar tema\n\n" +
                        "Enter → Enviar\n\n" +
                        "Ctrl + N → Nova conversa\n\n" +
                        "Ctrl + L → Limpar campo"
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

                    if (e.getCode() == KeyCode.ENTER &&
                            !e.isControlDown() &&
                            !e.isShiftDown()) {

                        enviarMensagem();

                        e.consume();
                    }


                    else if (
                            e.isControlDown() &&
                                    e.getCode() == KeyCode.N) {

                        novaConversa();

                        e.consume();
                    }


                    else if (
                            e.isControlDown() &&
                                    e.getCode() == KeyCode.L) {

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

    private String erroAmigavel(Throwable erro) {

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


        return "❌ Não foi possível obter uma resposta da IA.";
    }


    // =====================================================
    // SCROLL
    // =====================================================

    private void scroll() {

        Platform.runLater(() ->
                chatScroll.setVvalue(1)
        );
    }


    // =====================================================
    // LIBERAR INPUT
    // =====================================================

    private void liberar() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        campoMensagem.requestFocus();
    }
}