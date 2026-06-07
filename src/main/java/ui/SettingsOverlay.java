package ui;

import controller.LobbyController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

import java.io.IOException;
import java.util.Objects;

public final class SettingsOverlay {
    private static final double DEFAULT_WIDTH = 960;
    private static final double DEFAULT_HEIGHT = 640;

    private SettingsOverlay() {
    }

    public static StackPane wrap(Parent content, Stage stage) {
        StackPane root = new StackPane(content);
        addSettingsButton(root, stage);
        return root;
    }

    public static StackPane wrapGame(Parent content, Stage stage) {
        return wrapGame(content, stage, () -> {
        });
    }

    public static StackPane wrapGame(Parent content, Stage stage, Runnable beforeReturnToLobby) {
        StackPane root = new StackPane(content);
        addPauseButton(root, stage, beforeReturnToLobby);
        return root;
    }

    public static Scene createScene(Parent content, Stage stage) {
        return createScene(content, stage, false);
    }

    public static Scene createGameScene(Parent content, Stage stage) {
        return createGameScene(content, stage, () -> {
        });
    }

    public static Scene createGameScene(Parent content, Stage stage, Runnable beforeReturnToLobby) {
        return createScene(content, stage, true, beforeReturnToLobby);
    }

    public static void addTo(StackPane root, Stage stage) {
        addSettingsButton(root, stage);
    }

    public static void showLobby(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(SettingsOverlay.class.getResource("/ui/lobby-view.fxml"));
            Parent root = loader.load();
            LobbyController controller = loader.getController();
            controller.setStage(stage);
            stage.setTitle("Monopoly Deal - Lobby");
            stage.setScene(createScene(root, stage));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load lobby screen", e);
        }
    }

    private static Scene createScene(Parent content, Stage stage, boolean gameScreen) {
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        if (stage != null && stage.getScene() != null) {
            width = Math.max(stage.getMinWidth(), stage.getScene().getWidth());
            height = Math.max(stage.getMinHeight(), stage.getScene().getHeight());
        }
        return createScene(content, stage, gameScreen, () -> {
        });
    }

    private static Scene createScene(Parent content, Stage stage, boolean gameScreen,
                                     Runnable beforeReturnToLobby) {
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        if (stage != null && stage.getScene() != null) {
            width = Math.max(stage.getMinWidth(), stage.getScene().getWidth());
            height = Math.max(stage.getMinHeight(), stage.getScene().getHeight());
        }
        Parent root = gameScreen
                ? wrapGame(content, stage, beforeReturnToLobby)
                : wrap(content, stage);
        return new Scene(root, width, height);
    }

    private static void addSettingsButton(StackPane root, Stage stage) {
        Button settingsButton = new Button("\u2699");
        settingsButton.getStyleClass().add("settings-button");
        settingsButton.setOnAction(event -> showSettingsDialog(settingsButton, stage));
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(18, 22, 0, 0));
        root.getChildren().add(settingsButton);
    }

    private static void addPauseButton(StackPane root, Stage stage, Runnable beforeReturnToLobby) {
        Button pauseButton = new Button("\u23F8");
        pauseButton.getStyleClass().add("settings-button");
        pauseButton.getStyleClass().add("pause-button");
        pauseButton.setOnAction(event -> showPauseDialog(pauseButton, stage, beforeReturnToLobby));
        StackPane.setAlignment(pauseButton, Pos.TOP_RIGHT);
        StackPane.setMargin(pauseButton, new Insets(18, 22, 0, 0));
        root.getChildren().add(pauseButton);
    }

    private static void showSettingsDialog(Button owner, Stage stage) {
        Dialog<ButtonType> dialog = createDialog("Settings", owner, stage);
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(close);
        dialog.getDialogPane().setContent(createSettingsBody(
                "Game Settings",
                "Adjust global playback and display."));
        styleDialog(dialog);
        dialog.showAndWait();
    }

    private static void showPauseDialog(Button owner, Stage stage, Runnable beforeReturnToLobby) {
        Dialog<ButtonType> dialog = createDialog("Paused", owner, stage);
        ButtonType lobby = new ButtonType("Return to Lobby", ButtonBar.ButtonData.OTHER);
        ButtonType resume = new ButtonType("Resume", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(lobby, resume);
        dialog.getDialogPane().setContent(createSettingsBody(
                "Game Paused",
                "Resume the match, adjust playback, or return to the lobby."));
        styleDialog(dialog);

        dialog.showAndWait().ifPresent(result -> {
            if (result == lobby && stage != null) {
                if (beforeReturnToLobby != null) {
                    beforeReturnToLobby.run();
                }
                showLobby(stage);
            }
        });
    }

    private static Dialog<ButtonType> createDialog(String title, Button owner, Stage stage) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        if (stage != null) {
            dialog.initOwner(stage);
        } else if (owner.getScene() != null && owner.getScene().getWindow() != null) {
            dialog.initOwner(owner.getScene().getWindow());
        }
        return dialog;
    }

    private static VBox createSettingsBody(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("settings-dialog-title");
        Label subtitle = new Label(subtitleText);
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
        return body;
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
