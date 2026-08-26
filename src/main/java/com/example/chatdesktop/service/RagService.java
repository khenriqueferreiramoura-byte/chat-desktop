package com.example.chatdesktop.service;

import com.example.chatdesktop.model.DocumentChunk;
import com.example.chatdesktop.model.RagResult;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RagService {

    private final DocumentService documentService;

    private List<DocumentChunk> documentos;

    public RagService(DocumentService documentService) {

        this.documentService = documentService;

        recarregar();
    }

    // =====================================================
    // CARREGAR / RECARREGAR DOCUMENTOS
    // =====================================================

    public synchronized void recarregar() {

        documentos = documentService.carregarDocumentos();

        System.out.println(
                "\n========================================"
        );

        System.out.println(
                "RAG: documentos carregados = "
                        + documentos.size()
        );

        for (DocumentChunk documento : documentos) {

            System.out.println(
                    "RAG: "
                            + documento.getSource()
            );
        }

        System.out.println(
                "========================================\n"
        );
    }

    // =====================================================
    // BUSCAR DOCUMENTOS
    // =====================================================

    public synchronized List<RagResult> buscar(
            String pergunta,
            int quantidade
    ) {

        if (pergunta == null ||
                pergunta.isBlank()) {

            return List.of();
        }

        if (documentos == null ||
                documentos.isEmpty()) {

            System.out.println(
                    "RAG: nenhum documento disponível."
            );

            return List.of();
        }

        System.out.println(
                "\n[RAG] Pergunta: "
                        + pergunta
        );

        Set<String> termosPergunta =
                extrairTermos(pergunta);

        System.out.println(
                "[RAG] Termos: "
                        + termosPergunta
        );

        List<RagResult> resultados =
                new ArrayList<>();

        for (DocumentChunk documento : documentos) {

            double score =
                    calcularScore(
                            pergunta,
                            termosPergunta,
                            documento.getContent()
                    );

            System.out.println(
                    "[RAG] Documento: "
                            + documento.getSource()
                            + " | Score: "
                            + score
            );

            if (score > 0) {

                resultados.add(
                        new RagResult(
                                documento,
                                score
                        )
                );
            }
        }

        resultados.sort(
                Comparator.comparingDouble(
                        RagResult::getScore
                ).reversed()
        );

        List<RagResult> encontrados =
                resultados
                        .stream()
                        .limit(Math.max(1, quantidade))
                        .toList();

        System.out.println(
                "[RAG] Resultados encontrados: "
                        + encontrados.size()
        );

        for (RagResult resultado : encontrados) {

            System.out.println(
                    "[RAG] -> "
                            + resultado
                            .getChunk()
                            .getSource()
                            + " | Score: "
                            + resultado.getScore()
            );
        }

        return encontrados;
    }

    // =====================================================
    // CALCULAR SCORE
    // =====================================================

    private double calcularScore(
            String pergunta,
            Set<String> termosPergunta,
            String conteudo
    ) {

        if (conteudo == null ||
                conteudo.isBlank()) {

            return 0;
        }

        String perguntaNormalizada =
                normalizar(pergunta);

        String conteudoNormalizado =
                normalizar(conteudo);

        // =================================================
        // 1. CORRESPONDÊNCIA EXATA
        // =================================================

        if (!perguntaNormalizada.isBlank()
                && conteudoNormalizado.contains(
                perguntaNormalizada
        )) {

            return 10.0;
        }

        // =================================================
        // 2. NÚMEROS
        // =================================================

        Set<String> numerosPergunta =
                extrairNumeros(pergunta);

        Set<String> numerosDocumento =
                extrairNumeros(conteudo);

        int numerosEncontrados = 0;

        for (String numero : numerosPergunta) {

            if (numerosDocumento.contains(numero)) {

                numerosEncontrados++;
            }
        }

        // =================================================
        // 3. TERMOS
        // =================================================

        Set<String> termosDocumento =
                extrairTermos(conteudo);

        int encontrados = 0;

        for (String termo : termosPergunta) {

            if (termosDocumento.contains(termo)) {

                encontrados++;
            }
        }

        double cobertura = 0;

        if (!termosPergunta.isEmpty()) {

            cobertura =
                    (double) encontrados
                            / termosPergunta.size();
        }

        // =================================================
        // 4. SCORE DOS NÚMEROS
        // =================================================

        double scoreNumeros = 0;

        if (!numerosPergunta.isEmpty()) {

            scoreNumeros =
                    (double) numerosEncontrados
                            / numerosPergunta.size();
        }

        // =================================================
        // 5. COMBINAÇÃO
        // =================================================

        double score =
                cobertura
                        + (scoreNumeros * 2.0);

        return score;
    }

    // =====================================================
    // EXTRAIR TERMOS
    // =====================================================

    private Set<String> extrairTermos(
            String texto
    ) {

        String normalizado =
                normalizar(texto);

        if (normalizado.isBlank()) {

            return Set.of();
        }

        String[] palavras =
                normalizado.split("\\s+");

        Set<String> termos =
                new HashSet<>();

        for (String palavra : palavras) {

            if (palavra.length() >= 2) {

                termos.add(palavra);
            }
        }

        return termos;
    }

    // =====================================================
    // EXTRAIR NÚMEROS
    // =====================================================

    private Set<String> extrairNumeros(
            String texto
    ) {

        Set<String> numeros =
                new HashSet<>();

        if (texto == null ||
                texto.isBlank()) {

            return numeros;
        }

        String normalizado =
                normalizar(texto);

        String[] partes =
                normalizado.split("\\s+");

        for (String parte : partes) {

            if (parte.matches("\\d+")) {

                numeros.add(parte);
            }
        }

        return numeros;
    }

    // =====================================================
    // NORMALIZAR
    // =====================================================

    private String normalizar(
            String texto
    ) {

        if (texto == null) {

            return "";
        }

        String normalizado =
                Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                );

        normalizado =
                normalizado.replaceAll(
                        "\\p{M}",
                        ""
                );

        normalizado =
                normalizado
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9\\s]",
                                " "
                        );

        return normalizado
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    // =====================================================
    // CRIAR CONTEXTO
    // =====================================================

    public String criarContexto(
            String pergunta,
            int quantidade
    ) {

        List<RagResult> resultados =
                buscar(
                        pergunta,
                        quantidade
                );

        if (resultados.isEmpty()) {

            System.out.println(
                    "[RAG] Nenhum contexto encontrado."
            );

            return "";
        }

        StringBuilder contexto =
                new StringBuilder();

        for (int i = 0;
             i < resultados.size();
             i++) {

            RagResult resultado =
                    resultados.get(i);

            DocumentChunk chunk =
                    resultado.getChunk();

            contexto
                    .append("DOCUMENTO ")
                    .append(i + 1)
                    .append("\n");

            contexto
                    .append("Fonte: ")
                    .append(
                            chunk.getSource()
                    )
                    .append("\n");

            contexto
                    .append("Relevância: ")
                    .append(
                            resultado.getScore()
                    )
                    .append("\n\n");

            contexto
                    .append(
                            chunk.getContent()
                    )
                    .append("\n\n");

            contexto
                    .append(
                            "--------------------\n\n"
                    );
        }

        String resultadoFinal =
                contexto.toString();

        System.out.println(
                "\n[RAG] ===== CONTEXTO ENVIADO ====="
        );

        System.out.println(
                resultadoFinal
        );

        System.out.println(
                "[RAG] =============================\n"
        );

        return resultadoFinal;
    }
}