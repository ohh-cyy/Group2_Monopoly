package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ui.SettingsOverlay;

public class GameOverController {

    @FXML
    private Label winnerLabel;

    public void setWinner(String winnerName) {
        winnerLabel.setText(winnerName + " Wins!");
    }

    @FXML
    private void onReturnToMenu() throws Exception {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/ui/main-menu.fxml"));
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) winnerLabel.getScene().getRoot()).getScene().getWindow();
        javafx.scene.Scene scene = new javafx.scene.Scene(SettingsOverlay.wrap(loader.load(), stage));
        stage.setScene(scene);
        stage.setTitle("Monopoly Deal - Main Menu");
    }
}
