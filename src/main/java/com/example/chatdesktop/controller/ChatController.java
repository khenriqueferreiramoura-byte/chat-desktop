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

    private final GroqService groqService;

    private final List<ChatMessage> historico;

    public ChatController() {

        this.groqService = new GroqService();

        this.historico = new ArrayList<>();
    }

    @FXML
    private void initialize() {

        historico.add(
                new ChatMessage(
                        "system",
                        "Você é um assistente útil, educado e objetivo. " +
                                "Responda sempre em português do Brasil."
                )
        );

        campoMensagem.requestFocus();
    }

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

        adicionarMensagemNaTela(
                "Você",
                mensagem
        );

        historico.add(
                new ChatMessage(
                        "user",
                        mensagem
                )
        );

        bloquearInterface();

        groqService
                .enviarMensagem(historico)
                .thenAccept(this::receberResposta)
                .exceptionally(this::tratarErro);
    }

    private void receberResposta(
            String resposta
    ) {

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

    private Void tratarErro(
            Throwable erro
    ) {

        Throwable causa = erro.getCause() != null
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

    private void bloquearInterface() {

        campoMensagem.setDisable(true);

        botaoEnviar.setDisable(true);
    }

    private void liberarInterface() {

        campoMensagem.setDisable(false);

        botaoEnviar.setDisable(false);

        campoMensagem.requestFocus();
    }
}