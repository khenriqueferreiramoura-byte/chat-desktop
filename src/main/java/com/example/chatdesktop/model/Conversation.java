package com.example.chatdesktop.model;

public class Conversation {

    private int id;

    private String titulo;

    public Conversation(
            int id,
            String titulo
    ) {

        this.id = id;
        this.titulo = titulo;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(
            String titulo
    ) {

        this.titulo = titulo;
    }
}