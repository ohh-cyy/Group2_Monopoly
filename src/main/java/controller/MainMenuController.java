package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.SettingsOverlay;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private void onStartGame(ActionEvent event) throws IOException {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
        Parent root = loader.load();
        Scene scene = SettingsOverlay.createGameScene(root, stage);
        stage.setScene(scene);
        stage.setTitle("Monopoly Deal - Game");
    }

    @FXML
    private void onExit() {
        System.exit(0);
    }
}
