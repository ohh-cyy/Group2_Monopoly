package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public final class SettingsOverlay {
    private SettingsOverlay() {
    }

    public static StackPane wrap(Parent content, Stage stage) {
        StackPane root = new StackPane(content);
        Button settingsButton = new Button("⚙");
        settingsButton.getStyleClass().add("settings-button");
        settingsButton.setOnAction(event -> showSettingsDialog(settingsButton, stage));
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(18, 22, 0, 0));
        root.getChildren().add(settingsButton);
        return root;
    }

    public static void addTo(StackPane root, Stage stage) {
        Button settingsButton = new Button("⚙");
        settingsButton.getStyleClass().add("settings-button");
        settingsButton.setOnAction(event -> showSettingsDialog(settingsButton, stage));
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(18, 22, 0, 0));
        root.getChildren().add(settingsButton);
    }

    private static void showSettingsDialog(Button owner, Stage stage) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        if (stage != null) {
            dialog.initOwner(stage);
        } else if (owner.getScene() != null && owner.getScene().getWindow() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }

        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().setAll(close);

        Label title = new Label("Game Settings");
        title.getStyleClass().add("settings-dialog-title");
        Label subtitle = new Label("Adjust global playback and display.");
        subtitle.getStyleClass().add("settings-dialog-subtitle");

        CheckBox musicToggle = new CheckBox("Music");
        musicToggle.getStyleClass().add("settings-checkbox");
        musicToggle.setSelected(GameSettings.isMusicEnabled());
        musicToggle.selectedProperty().addListener((obs, oldValue, selected) ->
                GameSettings.setMusicEnabled(selected));

        CheckBox fullscreenToggle = new CheckBox("Fullscreen");
        fullscreenToggle.getStyleClass().add("settings-checkbox");
        fullscreenToggle.setSelected(GameSettings.isFullscreenEnabled());
        fullscreenToggle.selectedProperty().addListener((obs, oldValue, selected) ->
                GameSettings.setFullscreenEnabled(selected));

        VBox body = new VBox(14, title, subtitle, musicToggle, fullscreenToggle);
        body.getStyleClass().add("settings-dialog-body");
        pane.setContent(body);
        styleDialog(dialog);
        dialog.showAndWait();
    }

    private static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        try {
            String css = Objects.requireNonNull(
                    SettingsOverlay.class.getResource("/ui/game-theme.css")).toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
        pane.getStyleClass().add("game-dialog");
        pane.getStyleClass().add("settings-dialog");
    }
}
