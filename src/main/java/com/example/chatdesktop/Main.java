package com.example.chatdesktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        URL fxml = Main.class.getResource(
                "/view/chat-view.fxml"
        );

        if (fxml == null) {
            throw new IllegalStateException(
                    "FXML não encontrado: /view/chat-view.fxml"
            );
        }

        FXMLLoader loader = new FXMLLoader(fxml);

        Scene scene = new Scene(
                loader.load(),
                700,
                600
        );

        URL css = Main.class.getResource(
                "/css/chat.css"
        );

        if (css != null) {
            scene.getStylesheets().add(
                    css.toExternalForm()
            );
        }

        stage.setTitle("Chat JavaFX + Groq");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}