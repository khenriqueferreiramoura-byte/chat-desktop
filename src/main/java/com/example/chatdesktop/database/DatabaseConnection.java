package com.example.chatdesktop.database;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {


    private static final Path PASTA_BANCO =
            Path.of(
                    System.getProperty("user.home"),
                    "OrbitIA"
            );

    private static final Path ARQUIVO_BANCO =
            PASTA_BANCO.resolve(
                    "chat.db"
            );

    private static final String URL =
            "jdbc:sqlite:"
                    + ARQUIVO_BANCO.toAbsolutePath();

    private DatabaseConnection() {
    }

    public static Connection getConnection()
            throws SQLException {

        try {

            Files.createDirectories(
                    PASTA_BANCO
            );

        } catch (IOException e) {

            throw new SQLException(
                    "Não foi possível criar a pasta do banco.",
                    e
            );
        }

        return DriverManager.getConnection(URL);
    }


}
