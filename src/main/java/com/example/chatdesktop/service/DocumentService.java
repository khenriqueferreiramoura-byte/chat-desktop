package com.example.chatdesktop.service;

import com.example.chatdesktop.model.DocumentChunk;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DocumentService {

    private final Path pastaDocumentos;

    public DocumentService(Path pastaDocumentos) {
        this.pastaDocumentos = pastaDocumentos;
    }

    public List<DocumentChunk> carregarDocumentos() {

        List<DocumentChunk> documentos = new ArrayList<>();

        System.out.println();
        System.out.println("========================================");
        System.out.println("RAG - CARREGANDO DOCUMENTOS");
        System.out.println("========================================");

        System.out.println(
                "Pasta procurada: "
                        + pastaDocumentos.toAbsolutePath()
        );

        System.out.println(
                "Pasta existe: "
                        + Files.exists(pastaDocumentos)
        );

        if (!Files.exists(pastaDocumentos)) {

            System.err.println(
                    "ERRO: A pasta knowledge NÃO foi encontrada!"
            );

            System.out.println(
                    "========================================"
            );

            return documentos;
        }

        try (Stream<Path> arquivos =
                     Files.walk(pastaDocumentos)) {

            arquivos
                    .filter(Files::isRegularFile)
                    .forEach(arquivo -> {

                        System.out.println(
                                "Arquivo encontrado: "
                                        + arquivo
                        );

                        System.out.println(
                                "Suportado: "
                                        + arquivoSuportado(arquivo)
                        );

                        if (!arquivoSuportado(arquivo)) {
                            return;
                        }

                        try {

                            String conteudo =
                                    Files.readString(
                                            arquivo,
                                            StandardCharsets.UTF_8
                                    );

                            System.out.println(
                                    "Conteúdo: "
                                            + conteudo
                            );

                            adicionarChunks(
                                    documentos,
                                    arquivo,
                                    conteudo
                            );

                        } catch (IOException erro) {

                            System.err.println(
                                    "Não foi possível ler: "
                                            + arquivo
                            );

                            erro.printStackTrace();
                        }
                    });

        } catch (IOException erro) {

            throw new RuntimeException(
                    "Erro ao carregar documentos.",
                    erro
            );
        }

        System.out.println(
                "Total de chunks carregados: "
                        + documentos.size()
        );

        System.out.println(
                "========================================"
        );

        return documentos;
    }

    private boolean arquivoSuportado(Path arquivo) {

        String nome =
                arquivo.getFileName()
                        .toString()
                        .toLowerCase();

        return nome.endsWith(".txt");
    }

    private void adicionarChunks(
            List<DocumentChunk> documentos,
            Path arquivo,
            String conteudo
    ) {

        if (conteudo == null ||
                conteudo.isBlank()) {

            return;
        }

        /*
         * Tamanho aproximado de cada chunk.
         */
        int tamanhoChunk = 1200;

        /*
         * Sobreposição entre chunks.
         */
        int overlap = 200;

        int inicio = 0;

        while (inicio < conteudo.length()) {

            int fim = Math.min(
                    inicio + tamanhoChunk,
                    conteudo.length()
            );

            String chunk =
                    conteudo
                            .substring(inicio, fim)
                            .trim();

            if (!chunk.isBlank()) {

                documentos.add(
                        new DocumentChunk(
                                arquivo.toString(),
                                chunk
                        )
                );
            }

            if (fim >= conteudo.length()) {
                break;
            }

            inicio =
                    fim - overlap;
        }
    }
}