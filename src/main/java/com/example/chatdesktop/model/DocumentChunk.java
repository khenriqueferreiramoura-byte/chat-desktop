package com.example.chatdesktop.model;

public class DocumentChunk {

    private final String source;
    private final String content;

    public DocumentChunk(String source, String content) {
        this.source = source;
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "DocumentChunk{" +
                "source='" + source + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}