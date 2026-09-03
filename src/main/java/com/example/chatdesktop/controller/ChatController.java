package com.example.chatdesktop.controller;

import com.example.chatdesktop.dao.ChatDAO;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Priority;
import java.util.prefs.Preferences;
import com.example.chatdesktop.dao.ChatDAO;
import com.example.chatdesktop.model.Conversation;

import java.util.ArrayList;
import java.util.List;

import com.example.chatdesktop.model.Conversation;
public class ChatController {
    private final ChatDAO chatDAO = new ChatDAO();

    private final List<Conversation> conversasBanco =
            new ArrayList<>();

    private int conversaAtualId = -1;

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
    private boolean temaEscuro = true;

    private final Preferences preferencias =
            Preferences.userNodeForPackage(ChatController.class);
    private int idConversa = 0;


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

        chatDAO.criarTabelas();

        carregarHistoricoDoBanco();

        carregarTemaSalvo();

        iniciarHistorico();

        listaHistorico.setItems(
                listaConversas
        );

        configurarListaHistorico();

        listaHistorico.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2) {

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
    private void carregarHistoricoDoBanco() {



            conversasBanco.clear();

            listaConversas.clear();

            conversasBanco.addAll(
                    chatDAO.listarConversas()
            );

            for (Conversation conversa : conversasBanco) {

                listaConversas.add(
                        conversa.getTitulo()
                );
            }
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
                                        "user".equals(
                                                m.getRole()
                                        )
                        );


        // =================================================
        // CRIA A CONVERSA NO SQLITE
        // =================================================

        if (conversaAtualId == -1) {

            String titulo =
                    gerarTituloDaPrimeiraMensagem(
                            texto
                    );

            conversaAtualId =
                    chatDAO.criarConversa(
                            titulo
                    );

            carregarHistoricoDoBanco();
        }


        // =================================================
        // GUARDA O ID DA CONVERSA ATUAL
        // =================================================

        final int conversaId =
                conversaAtualId;


        // =================================================
        // UI
        // =================================================

        adicionarUsuario(texto);


        // =================================================
        // CRIA A MENSAGEM DO USUÁRIO
        // =================================================

        ChatMessage mensagemUsuario =
                new ChatMessage(
                        "user",
                        texto
                );


        // =================================================
        // HISTÓRICO EM MEMÓRIA
        // =================================================

        historico.add(
                mensagemUsuario
        );


        // =================================================
        // SALVA NO SQLITE
        // =================================================

        chatDAO.salvarMensagem(
                conversaId,
                mensagemUsuario
        );


        // =================================================
        // BLOQUEIA INPUT
        // =================================================

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);


        // =================================================
        // TÍTULO AUTOMÁTICO
        // =================================================

        if (primeiraMensagem &&
                !tituloGerado) {

            gerarTituloAutomaticamente(
                    texto,
                    conversaId
            );
        }


        // =================================================
        // RAG → WEB FALLBACK
        // =================================================

        CompletableFuture
                .supplyAsync(() ->
                        rag.criarContexto(
                                texto,
                                5
                        )
                )

                .thenCompose(contexto -> {

                    if (contexto != null &&
                            !contexto.isBlank()) {

                        System.out.println(
                                "📚 RAG encontrou informações."
                        );

                        return groq.enviarMensagem(
                                criarMensagensParaIA(
                                        texto,
                                        contexto
                                )
                        );
                    }

                    System.out.println(
                            "🌐 Ativando pesquisa na internet."
                    );

                    return groq.pesquisarNaInternet(
                            texto
                    );
                })

                // =================================================
                // RESPOSTA FINAL
                // =================================================

                .thenAccept(resposta -> {

                    /*
                     * Evita mostrar a resposta em outra conversa.
                     */
                    if (conversaId != conversaAtualId) {
                        return;
                    }

                    ChatMessage mensagemIA =
                            new ChatMessage(
                                    "assistant",
                                    resposta
                            );


                    // HISTÓRICO EM MEMÓRIA
                    historico.add(
                            mensagemIA
                    );


                    // SQLITE
                    chatDAO.salvarMensagem(
                            conversaId,
                            mensagemIA
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

                    if (conversaId != conversaAtualId) {
                        return null;
                    }

                    Throwable causa =
                            obterCausa(
                                    erro
                            );

                    Platform.runLater(() -> {

                        adicionarIA(
                                erroAmigavel(
                                        causa
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

        label.setMaxWidth(
                550
        );

        label.getStyleClass()
                .add(
                        "ai-message"
                );


        Label robo =
                new Label(
                        "🤖"
                );


        HBox linha =
                new HBox(
                        10,
                        label,
                        robo
                );

        linha.setAlignment(
                Pos.CENTER_LEFT
        );


        // =================================================
        // BOTÃO COPIAR
        // =================================================

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


        // =================================================
        // BOTÃO REGENERAR
        // =================================================

        Button regenerar =
                new Button(
                        "↻ Regenerar"
                );

        regenerar.getStyleClass()
                .add(
                        "regenerate-button"
                );


        // =================================================
        // BOTÕES
        // =================================================

        HBox botoes =
                new HBox(
                        8,
                        copiar,
                        regenerar
                );

        botoes.setAlignment(
                Pos.CENTER_LEFT
        );


        // =================================================
        // BLOCO DA RESPOSTA
        // =================================================

        VBox bloco =
                new VBox(
                        5,
                        linha,
                        botoes
                );

        bloco.setAlignment(
                Pos.CENTER_LEFT
        );


        // =================================================
        // EVENTO REGENERAR
        // =================================================

        regenerar.setOnAction(e -> {

            /*
             * Procura a última pergunta do usuário.
             */
            String ultimaPergunta = null;

            for (int i = historico.size() - 1;
                 i >= 0;
                 i--) {

                ChatMessage mensagem =
                        historico.get(i);

                if (mensagem.getRole()
                        .equals("user")) {

                    ultimaPergunta =
                            mensagem.getContent();

                    break;
                }
            }

            /*
             * Não encontrou pergunta.
             */
            if (ultimaPergunta == null ||
                    ultimaPergunta.isBlank()) {

                return;
            }

            regenerarResposta(
                    ultimaPergunta,
                    bloco
            );
        });


        // =================================================
        // ADICIONAR AO CHAT
        // =================================================

        chatContainer
                .getChildren()
                .add(
                        bloco
                );

        scroll();
    }

    // =====================================================
    // NOVA CONVERSA
    // =====================================================

    @FXML
    private void novaConversa() {

        conversaAtualId = -1;

        chatContainer.getChildren().clear();

        iniciarHistorico();

        adicionarIA(
                "🍥 Seja bem-vindo à Orbit-IA!\n\n"
                        + "Como posso ajudar você?"
        );

        listaHistorico
                .getSelectionModel()
                .clearSelection();

        campoMensagem.requestFocus();
    }


    // =====================================================
    // CARREGAR CONVERSA
    // =====================================================

    private void carregarConversa(
            int indice
    ) {

        if (indice < 0 ||
                indice >= conversasBanco.size()) {

            return;
        }


        // =================================================
        // CANCELA RESPOSTAS DA CONVERSA ANTERIOR
        // =================================================

        idConversa++;


        // =================================================
        // PEGA A CONVERSA DO BANCO
        // =================================================

        Conversation conversa =
                conversasBanco.get(
                        indice
                );


        conversaAtualId =
                conversa.getId();


        tituloConversaAtual =
                conversa.getTitulo();


        // =================================================
        // CARREGA MENSAGENS DO SQLITE
        // =================================================

        List<ChatMessage> mensagens =
                chatDAO.buscarMensagens(
                        conversaAtualId
                );


        // =================================================
        // LIMPA MEMÓRIA
        // =================================================

        iniciarHistorico();

        historico.addAll(
                mensagens
        );


        // =================================================
        // LIMPA TELA
        // =================================================

        chatContainer
                .getChildren()
                .clear();


        // =================================================
        // MOSTRA MENSAGENS
        // =================================================

        for (ChatMessage mensagem :
                mensagens) {

            if ("user".equals(
                    mensagem.getRole()
            )) {

                adicionarUsuario(
                        mensagem.getContent()
                );

            } else if ("assistant".equals(
                    mensagem.getRole()
            )) {

                adicionarIA(
                        mensagem.getContent()
                );
            }
        }


        tituloGerado = true;

        gerandoTitulo = false;


        // =================================================
        // SELECIONA A CONVERSA
        // =================================================

        listaHistorico
                .getSelectionModel()
                .select(
                        indice
                );


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

        scene.getStylesheets().remove(
                urlAntigo.toExternalForm()
        );

        scene.getStylesheets().add(
                urlNovo.toExternalForm()
        );

        temaEscuro =
                !temaEscuro;

        // SALVA O TEMA ESCOLHIDO
        preferencias.putBoolean(
                "temaEscuro",
                temaEscuro
        );

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
                    } else if (
                            e.isControlDown()
                                    &&
                                    e.getCode()
                                            == KeyCode.N
                    ) {

                        novaConversa();

                        e.consume();
                    } else if (
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
    // =====================================================
// REGENERAR RESPOSTA
// =====================================================

    private void regenerarResposta(
            String pergunta,
            VBox bloco
    ) {

        if (campoMensagem.isDisabled()) {
            return;
        }

        int id = idConversa;

        /*
         * Remove a resposta antiga do histórico.
         */
        if (!historico.isEmpty()) {

            ChatMessage ultimaMensagem =
                    historico.get(
                            historico.size() - 1
                    );

            if (ultimaMensagem.getRole()
                    .equals("assistant")) {

                historico.remove(
                        historico.size() - 1
                );
            }
        }

        /*
         * Remove a resposta antiga da tela.
         */
        chatContainer
                .getChildren()
                .remove(
                        bloco
                );

        /*
         * Bloqueia o usuário enquanto gera novamente.
         */
        campoMensagem.setDisable(true);
        botaoEnviar.setDisable(true);

        /*
         * Mensagem temporária.
         */
        adicionarIA(
                "🔄 Regenerando resposta..."
        );

        /*
         * Remove a mensagem temporária.
         */
        VBox mensagemCarregando =
                (VBox) chatContainer
                        .getChildren()
                        .get(
                                chatContainer
                                        .getChildren()
                                        .size() - 1
                        );

        // =================================================
        // RAG
        // =================================================

        CompletableFuture
                .supplyAsync(() ->
                        rag.criarContexto(
                                pergunta,
                                5
                        )
                )

                .thenCompose(contexto -> {

                    if (contexto != null &&
                            !contexto.isBlank()) {

                        System.out.println();
                        System.out.println(
                                "🔄 Regenerando usando RAG."
                        );

                        return groq.enviarMensagem(
                                criarMensagensParaIA(
                                        pergunta,
                                        contexto
                                )
                        );
                    }

                    System.out.println();
                    System.out.println(
                            "🔄 Regenerando usando pesquisa web."
                    );

                    return groq.pesquisarNaInternet(
                            pergunta
                    );
                })

                .thenAccept(resposta -> {

                    if (id != idConversa) {
                        return;
                    }

                    /*
                     * Salva a nova resposta.
                     */
                    historico.add(
                            new ChatMessage(
                                    "assistant",
                                    resposta
                            )
                    );

                    Platform.runLater(() -> {

                        /*
                         * Remove "Regenerando..."
                         */
                        chatContainer
                                .getChildren()
                                .remove(
                                        mensagemCarregando
                                );

                        /*
                         * Mostra a nova resposta.
                         */
                        adicionarIA(
                                resposta
                        );

                        liberar();
                    });
                })

                .exceptionally(erro -> {

                    if (id != idConversa) {
                        return null;
                    }

                    Platform.runLater(() -> {

                        chatContainer
                                .getChildren()
                                .remove(
                                        mensagemCarregando
                                );

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
// EXCLUIR CONVERSA
// =====================================================

    // =====================================================
// EXCLUIR CONVERSA
// =====================================================

    // =====================================================
// EXCLUIR CONVERSA COM CONFIRMAÇÃO
// =====================================================

    private void excluirConversa(
            int indice
    ) {

        if (indice < 0 ||
                indice >= conversasBanco.size()) {

            return;
        }


        // =========================================
        // PEGA A CONVERSA DO BANCO
        // =========================================

        Conversation conversa =
                conversasBanco.get(
                        indice
                );

        int conversaId =
                conversa.getId();

        String titulo =
                conversa.getTitulo();


        // =========================================
        // CONFIRMAÇÃO
        // =========================================

        Alert confirmacao =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmacao.setTitle(
                "Excluir conversa"
        );

        confirmacao.setHeaderText(
                "Deseja realmente excluir esta conversa?"
        );

        confirmacao.setContentText(
                "A conversa \"" +
                        titulo +
                        "\" será removida do histórico."
        );


        // =========================================
        // CONFIRMAR
        // =========================================

        confirmacao.showAndWait()
                .ifPresent(botao -> {

                    if (botao !=
                            javafx.scene.control.ButtonType.OK) {

                        return;
                    }


                    // =====================================
                    // EXCLUI DO SQLITE
                    // =====================================

                    chatDAO.excluirConversa(
                            conversaId
                    );


                    // =====================================
                    // ATUALIZA LISTA
                    // =====================================

                    conversasBanco.remove(
                            indice
                    );

                    listaConversas.remove(
                            indice
                    );


                    // =====================================
                    // SE ERA A CONVERSA ATUAL
                    // =====================================

                    if (conversaAtualId == conversaId) {

                        conversaAtualId = -1;

                        idConversa++;

                        chatContainer
                                .getChildren()
                                .clear();

                        iniciarHistorico();

                        adicionarIA(
                                "🍥 Seja bem-vindo à Orbit-IA!\n\n"
                                        + "Como posso ajudar você?"
                        );
                    }


                    // =====================================
                    // LIMPA SELEÇÃO
                    // =====================================

                    listaHistorico
                            .getSelectionModel()
                            .clearSelection();


                    System.out.println(
                            "🗑 Conversa excluída do SQLite: "
                                    + titulo
                    );
                });
    }
    // =====================================================
// CONFIGURAR LISTA DO HISTÓRICO
// =====================================================

    // =====================================================
// CONFIGURAR LISTA DO HISTÓRICO
// =====================================================

    private void configurarListaHistorico() {

        listaHistorico.setCellFactory(
                listView -> new ListCell<String>() {

                    private final Label titulo =
                            new Label();

                    private final Button renomear =
                            new Button("✏");

                    private final Button excluir =
                            new Button("🗑");

                    private final HBox linha =
                            new HBox(
                                    8,
                                    titulo,
                                    renomear,
                                    excluir
                            );


                    {
                        // =========================================
                        // TÍTULO
                        // =========================================

                        titulo.setMaxWidth(
                                Double.MAX_VALUE
                        );

                        HBox.setHgrow(
                                titulo,
                                Priority.ALWAYS
                        );

                        titulo.getStyleClass().add(
                                "history-item-title"
                        );


                        // =========================================
                        // RENOMEAR
                        // =========================================

                        renomear.getStyleClass().add(
                                "rename-history-button"
                        );

                        renomear.setOnAction(event -> {

                            int indice =
                                    getIndex();

                            if (indice >= 0 &&
                                    indice < listaConversas.size()) {

                                renomearConversa(
                                        indice
                                );
                            }

                            event.consume();
                        });


                        // =========================================
                        // EXCLUIR
                        // =========================================

                        excluir.getStyleClass().add(
                                "delete-history-button"
                        );

                        excluir.setOnAction(event -> {

                            int indice = getIndex();

                            if (indice >= 0 &&
                                    indice < conversasBanco.size()) {

                                excluirConversa(indice);
                            }

                            event.consume();
                        });


                        // =========================================
                        // LAYOUT
                        // =========================================

                        linha.setAlignment(
                                Pos.CENTER_LEFT
                        );

                        linha.getStyleClass().add(
                                "history-item"
                        );
                    }


                    // =============================================
                    // ATUALIZAR ITEM
                    // =============================================

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty
                    ) {

                        super.updateItem(
                                item,
                                empty
                        );


                        if (empty || item == null) {

                            setText(null);

                            setGraphic(null);

                        } else {

                            titulo.setText(
                                    item
                            );

                            setText(null);

                            setGraphic(
                                    linha
                            );
                        }
                    }
                }
        );
    }
    // =====================================================
// RENOMEAR CONVERSA
// =====================================================

    private void renomearConversa(
            int indice
    ) {

        if (indice < 0 ||
                indice >= listaConversas.size()) {

            return;
        }


        String tituloAtual =
                listaConversas.get(
                        indice
                );


        TextInputDialog dialog =
                new TextInputDialog(
                        tituloAtual
                );


        dialog.setTitle(
                "Renomear conversa"
        );

        dialog.setHeaderText(
                "Digite um novo título para a conversa"
        );

        dialog.setContentText(
                "Novo Título:"
        );


        dialog.showAndWait()
                .ifPresent(novoTitulo -> {

                    novoTitulo =
                            novoTitulo.trim();


                    if (novoTitulo.isEmpty()) {

                        return;
                    }


                    // =========================================
                    // LIMITAR TAMANHO
                    // =========================================

                    if (novoTitulo.length() > 40) {

                        novoTitulo =
                                novoTitulo.substring(
                                        0,
                                        40
                                );
                    }


                    // =========================================
                    // ATUALIZAR LISTA
                    // =========================================

                    listaConversas.set(
                            indice,
                            novoTitulo
                    );


                    // =========================================
                    // ATUALIZAR CONVERSA ATUAL
                    // =========================================

                    if (listaHistorico
                            .getSelectionModel()
                            .getSelectedIndex()
                            == indice) {

                        tituloConversaAtual =
                                novoTitulo;
                    }


                    System.out.println(
                            "✏ Conversa renomeada para: "
                                    + novoTitulo
                    );
                });
    }

    private void carregarTemaSalvo() {

        temaEscuro =
                preferencias.getBoolean(
                        "temaEscuro",
                        true
                );

        Platform.runLater(() -> {

            Scene scene =
                    chatContainer.getScene();

            if (scene == null) {
                return;
            }

            java.net.URL urlClaro =
                    getClass().getResource(CLARO);

            java.net.URL urlEscuro =
                    getClass().getResource(ESCURO);

            if (urlClaro == null ||
                    urlEscuro == null) {

                System.err.println(
                        "CSS dos temas não encontrado."
                );

                return;
            }

            scene.getStylesheets().remove(
                    urlClaro.toExternalForm()
            );

            scene.getStylesheets().remove(
                    urlEscuro.toExternalForm()
            );

            String tema =
                    temaEscuro
                            ? urlEscuro.toExternalForm()
                            : urlClaro.toExternalForm();

            scene.getStylesheets().add(tema);

            botaoTema.setText(
                    temaEscuro
                            ? "☀"
                            : "🌙"
            );
        });
    }
    private String gerarTituloDaPrimeiraMensagem(
            String texto
    ) {

        texto = texto.trim();

        int tamanhoMaximo = 40;

        if (texto.length() <= tamanhoMaximo) {
            return texto;
        }

        return texto.substring(
                0,
                tamanhoMaximo
        ).trim() + "...";
    }
}




