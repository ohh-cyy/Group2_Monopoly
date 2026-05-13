package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private void onStartGame(ActionEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 800);
        stage.setScene(scene);
        stage.setTitle("Monopoly Deal - Game");
    }

    @FXML
    private void onExit() {
        System.exit(0);
    }
}
