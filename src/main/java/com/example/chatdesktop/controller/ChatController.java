package com.example.chatdesktop.controller;

import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.service.GroqService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;

public class ChatController {

    @FXML
    private TextArea areaChat;

    @FXML
    private TextField campoMensagem;

    @FXML
    private Button botaoEnviar;

    @FXML
    private Button botaoNovaConversa;


    private final GroqService groqService;

    private final List<ChatMessage> historico;


    /*
     * Identifica qual conversa está ativa.
     *
     * Isso evita que uma resposta antiga da Groq
     * apareça depois que o usuário iniciou uma
     * nova conversa.
     */
    private int idConversa = 0;


    /*
     * Prompt principal da IA.
     */
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
         * Mostra a mensagem do usuário.
         */

        adicionarMensagemNaTela(
                "Você",
                mensagem
        );


        /*
         * Adiciona ao histórico da IA.
         */

        historico.add(
                new ChatMessage(
                        "user",
                        mensagem
                )
        );


        /*
         * Guarda qual conversa fez a requisição.
         */

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
     * NOVA CONVERSA
     * =====================================================
     */

    @FXML
    private void novaConversa() {

        /*
         * Cria um novo ID.
         *
         * Qualquer resposta da conversa anterior
         * será considerada inválida.
         */

        idConversa++;


        /*
         * Limpa as mensagens da tela.
         */

        areaChat.clear();


        /*
         * Limpa o histórico enviado para a Groq.
         */

        historico.clear();


        /*
         * Adiciona novamente o prompt do sistema.
         */

        historico.add(
                new ChatMessage(
                        "system",
                        SYSTEM_PROMPT
                )
        );


        /*
         * Limpa o campo de texto.
         */

        campoMensagem.clear();


        /*
         * Garante que a interface esteja liberada.
         */

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);


        /*
         * Coloca o cursor novamente no campo.
         */

        campoMensagem.requestFocus();


        /*
         * Mensagem visual indicando nova conversa.
         */

        adicionarMensagemNaTela(
                "Sistema",
                "✨ Nova conversa iniciada."
        );
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
         * Se o usuário criou uma nova conversa enquanto
         * a Groq ainda estava respondendo, ignoramos
         * a resposta antiga.
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

            adicionarMensagemNaTela(
                    "IA",
                    resposta
            );

            liberarInterface();
        });
    }


    /*
     * =====================================================
     * TRATAR ERRO
     * =====================================================
     */

    private Void tratarErro(
            Throwable erro,
            int conversaDaRequisicao
    ) {

        /*
         * Ignora erros de uma conversa antiga.
         */

        if (conversaDaRequisicao != idConversa) {
            return null;
        }


        Throwable causa =
                erro.getCause() != null
                        ? erro.getCause()
                        : erro;


        Platform.runLater(() -> {

            adicionarMensagemNaTela(
                    "Erro",
                    causa.getMessage()
            );

            liberarInterface();
        });


        return null;
    }


    /*
     * =====================================================
     * ADICIONAR MENSAGEM NA TELA
     * =====================================================
     */

    private void adicionarMensagemNaTela(
            String autor,
            String mensagem
    ) {

        areaChat.appendText(
                autor
                        + ":\n"
                        + mensagem
                        + "\n\n"
        );
    }


    /*
     * =====================================================
     * BLOQUEAR INTERFACE
     * =====================================================
     */

    private void bloquearInterface() {

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);

        /*
         * Mantemos o botão Nova conversa disponível.
         *
         * Assim o usuário pode abandonar uma conversa
         * mesmo enquanto a IA está processando.
         */

        botaoNovaConversa.setDisable(false);
    }


    /*
     * =====================================================
     * LIBERAR INTERFACE
     * =====================================================
     */

    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        botaoNovaConversa.setDisable(false);

        campoMensagem.requestFocus();
    }
}