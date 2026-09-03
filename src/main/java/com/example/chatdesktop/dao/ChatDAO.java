package com.example.chatdesktop.dao;

import com.example.chatdesktop.database.DatabaseConnection;
import com.example.chatdesktop.model.ChatMessage;
import com.example.chatdesktop.model.Conversation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class ChatDAO {


    public void criarTabelas() {

        String sqlConversas =
                """
                CREATE TABLE IF NOT EXISTS conversas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT NOT NULL,
                    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;

        String sqlMensagens =
                """
                CREATE TABLE IF NOT EXISTS mensagens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    conversa_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    criado_em DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (conversa_id)
                    REFERENCES conversas(id)
                    ON DELETE CASCADE
                )
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             Statement statement =
                     connection.createStatement()) {

            statement.execute(
                    "PRAGMA foreign_keys = ON"
            );

            statement.execute(sqlConversas);

            statement.execute(sqlMensagens);

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao criar tabelas SQLite.",
                    e
            );
        }
    }


    public int criarConversa(
            String titulo
    ) {

        String sql =
                """
                INSERT INTO conversas (titulo)
                VALUES (?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setString(
                    1,
                    titulo
            );

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {

                    return keys.getInt(1);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao criar conversa.",
                    e
            );
        }

        return -1;
    }


    public void salvarMensagem(
            int conversaId,
            ChatMessage mensagem
    ) {

        String sql =
                """
                INSERT INTO mensagens (
                    conversa_id,
                    role,
                    content
                )
                VALUES (?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    conversaId
            );

            statement.setString(
                    2,
                    mensagem.getRole()
            );

            statement.setString(
                    3,
                    mensagem.getContent()
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao salvar mensagem.",
                    e
            );
        }
    }


    public List<Conversation>
    listarConversas() {

        List<Conversation> conversas =
                new ArrayList<>();

        String sql =
                """
                SELECT id, titulo
                FROM conversas
                ORDER BY id DESC
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                conversas.add(
                        new Conversation(
                                result.getInt("id"),
                                result.getString("titulo")
                        )
                );
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao carregar conversas.",
                    e
            );
        }

        return conversas;
    }


    public List<ChatMessage>
    buscarMensagens(
            int conversaId
    ) {

        List<ChatMessage> mensagens =
                new ArrayList<>();

        String sql =
                """
                SELECT role, content
                FROM mensagens
                WHERE conversa_id = ?
                ORDER BY id ASC
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    conversaId
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                while (result.next()) {

                    mensagens.add(
                            new ChatMessage(
                                    result.getString("role"),
                                    result.getString("content")
                            )
                    );
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao carregar mensagens.",
                    e
            );
        }

        return mensagens;
    }


    public void atualizarTitulo(
            int conversaId,
            String novoTitulo
    ) {

        String sql =
                """
                UPDATE conversas
                SET titulo = ?
                WHERE id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    novoTitulo
            );

            statement.setInt(
                    2,
                    conversaId
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao atualizar título.",
                    e
            );
        }
    }


    public void excluirConversa(
            int conversaId
    ) {

        String sql =
                """
                DELETE FROM conversas
                WHERE id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    conversaId
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Erro ao excluir conversa.",
                    e
            );
        }
    }

}
