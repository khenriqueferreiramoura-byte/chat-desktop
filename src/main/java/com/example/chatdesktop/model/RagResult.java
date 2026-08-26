package com.example.chatdesktop.model;

public class RagResult {

    private final DocumentChunk chunk;
    private final double score;

    public RagResult(DocumentChunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }

    public DocumentChunk getChunk() {
        return chunk;
    }

    public double getScore() {
        return score;
    }
}