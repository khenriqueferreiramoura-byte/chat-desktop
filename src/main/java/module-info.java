module com.example.chatdesktop {

    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;
    requires java.prefs;
    requires java.sql;

    exports com.example.chatdesktop;

    opens com.example.chatdesktop.controller
            to javafx.fxml;

}