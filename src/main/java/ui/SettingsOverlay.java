package ui;

import controller.LobbyController;
import controller.GameController;
import controller.NetworkGameController;
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
import network.client.NetworkClient;
import network.protocol.GameStateDto;

import java.io.IOException;
import java.util.Objects;

/**
 * Wraps game and lobby scenes with overlay controls for settings, pause, and navigation.
 * <p>
 * Provides factory methods to create scenes, open local or online games, and return to the lobby.
 */
public final class SettingsOverlay {
    private static final double DEFAULT_WIDTH = 960;
    private static final double DEFAULT_HEIGHT = 640;

    private SettingsOverlay() {
    }

    /** Wraps {@code content} with a top-right settings button for non-game screens. */
    public static StackPane wrap(Parent content, Stage stage) {
        StackPane root = new StackPane(content);
        addSettingsButton(root, stage);
        return root;
    }

    /** Wraps {@code content} with a pause button for in-game screens. */
    public static StackPane wrapGame(Parent content, Stage stage) {
        return wrapGame(content, stage, () -> {
        }, null, null);
    }

    /**
     * Wraps {@code content} with a pause button and runs {@code beforeReturnToLobby}
     * before navigating back to the lobby.
     */
    public static StackPane wrapGame(Parent content, Stage stage, Runnable beforeReturnToLobby) {
        return wrapGame(content, stage, beforeReturnToLobby, null, null);
    }

    /**
     * Wraps {@code content} with a pause button and optional pause/resume hooks
     * for freezing the turn timer while the dialog is open.
     */
    public static StackPane wrapGame(Parent content, Stage stage, Runnable beforeReturnToLobby,
                                     Runnable onPause, Runnable onResume) {
        StackPane root = new StackPane(content);
        addPauseButton(root, stage, beforeReturnToLobby, onPause, onResume);
        return root;
    }

    /** Creates a lobby-style scene with settings overlay and default dimensions. */
    public static Scene createScene(Parent content, Stage stage) {
        return createScene(content, stage, false);
    }

    /** Creates an in-game scene with pause overlay and default dimensions. */
    public static Scene createGameScene(Parent content, Stage stage) {
        return createGameScene(content, stage, () -> {
        });
    }

    /** Creates an in-game scene with pause overlay and a lobby-return hook. */
    public static Scene createGameScene(Parent content, Stage stage, Runnable beforeReturnToLobby) {
        return createGameScene(content, stage, beforeReturnToLobby, null, null);
    }

    /** Creates an in-game scene with pause/resume hooks for the turn timer. */
    public static Scene createGameScene(Parent content, Stage stage, Runnable beforeReturnToLobby,
                                        Runnable onPause, Runnable onResume) {
        return createScene(content, stage, true, beforeReturnToLobby, onPause, onResume);
    }

    /** Adds a settings button to an existing {@link StackPane} root. */
    public static void addTo(StackPane root, Stage stage) {
        addSettingsButton(root, stage);
    }

    /** Loads the lobby FXML view and switches the stage to it. */
    public static void showLobby(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(SettingsOverlay.class.getResource("/ui/lobby-view.fxml"));
            Parent root = loader.load();
            LobbyController controller = loader.getController();
            controller.setStage(stage);
            stage.setTitle("Monopoly Deal - Lobby");
            stage.setScene(createScene(root, stage));
            GameSettings.useLobbyMusic();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load lobby screen", e);
        }
    }

    /** Opens a local four-player game and returns the loaded controller. */
    public static GameController openLocalGame(Stage stage) throws IOException {
        return openLocalGame(stage, 4);
    }

    /** Opens a local game with the given player count and returns the loaded controller. */
    public static GameController openLocalGame(Stage stage, int playerCount) throws IOException {
        FXMLLoader loader = new FXMLLoader(SettingsOverlay.class.getResource("/ui/game-view.fxml"));
        loader.load();
        GameController controller = loader.getController();
        controller.startLocalGame(playerCount);
        stage.setTitle("Monopoly Deal - Local");
        stage.setScene(createGameScene(loader.getRoot(), stage, () -> {
        }, controller::pauseGame, controller::resumeGame));
        GameSettings.useGameMusic();
        return controller;
    }

    /**
     * Opens the online game view, wires the network client, and starts with {@code initialState}.
     *
     * @return the loaded {@link NetworkGameController}
     */
    public static NetworkGameController openNetworkGame(Stage stage,
                                                          NetworkClient client,
                                                          int localSeat,
                                                          GameStateDto initialState,
                                                          Runnable beforeReturnToLobby) throws IOException {
        var resource = SettingsOverlay.class.getResource("/ui/network-game-view.fxml");
        if (resource == null) {
            throw new IOException("missing network-game-view.fxml");
        }
        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        NetworkGameController controller = loader.getController();
        if (controller == null) {
            throw new IllegalStateException("NetworkGameController not loaded");
        }
        stage.setTitle("Monopoly Deal - Online");
        stage.setScene(createGameScene(root, stage, beforeReturnToLobby,
                controller::pauseGame, controller::resumeGame));
        GameSettings.useGameMusic();
        stage.show();
        controller.startOnlineGame(client, localSeat, initialState);
        return controller;
    }

    private static Scene createScene(Parent content, Stage stage, boolean gameScreen) {
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        if (stage != null && stage.getScene() != null) {
            width = Math.max(stage.getMinWidth(), stage.getScene().getWidth());
            height = Math.max(stage.getMinHeight(), stage.getScene().getHeight());
        }
        return createScene(content, stage, gameScreen, () -> {
        }, null, null);
    }

    private static Scene createScene(Parent content, Stage stage, boolean gameScreen,
                                     Runnable beforeReturnToLobby) {
        return createScene(content, stage, gameScreen, beforeReturnToLobby, null, null);
    }

    private static Scene createScene(Parent content, Stage stage, boolean gameScreen,
                                     Runnable beforeReturnToLobby, Runnable onPause, Runnable onResume) {
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        if (stage != null && stage.getScene() != null) {
            width = Math.max(stage.getMinWidth(), stage.getScene().getWidth());
            height = Math.max(stage.getMinHeight(), stage.getScene().getHeight());
        }
        Parent root = gameScreen
                ? wrapGame(content, stage, beforeReturnToLobby, onPause, onResume)
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

    private static void addPauseButton(StackPane root, Stage stage, Runnable beforeReturnToLobby,
                                       Runnable onPause, Runnable onResume) {
        Button pauseButton = new Button("\u23F8");
        pauseButton.getStyleClass().add("settings-button");
        pauseButton.getStyleClass().add("pause-button");
        pauseButton.setOnAction(event -> showPauseDialog(
                pauseButton, stage, beforeReturnToLobby, onPause, onResume));
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

    private static void showPauseDialog(Button owner, Stage stage, Runnable beforeReturnToLobby,
                                        Runnable onPause, Runnable onResume) {
        Dialog<ButtonType> dialog = createDialog("Paused", owner, stage);
        ButtonType lobby = new ButtonType("Return to Lobby", ButtonBar.ButtonData.OTHER);
        ButtonType resume = new ButtonType("Resume", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(lobby, resume);
        dialog.getDialogPane().setContent(createSettingsBody(
                "Game Paused",
                "Resume the match, adjust playback, or return to the lobby."));
        styleDialog(dialog);

        if (onPause != null) {
            onPause.run();
        }
        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() == lobby && stage != null) {
            if (beforeReturnToLobby != null) {
                beforeReturnToLobby.run();
            }
            showLobby(stage);
            return;
        }
        if (onResume != null) {
            onResume.run();
        }
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

        CheckBox soundToggle = new CheckBox("Sound Effects");
        soundToggle.getStyleClass().add("settings-checkbox");
        soundToggle.setSelected(GameSettings.isSoundEffectsEnabled());
        soundToggle.selectedProperty().addListener((obs, oldValue, selected) ->
                GameSettings.setSoundEffectsEnabled(selected));

        CheckBox fullscreenToggle = new CheckBox("Fullscreen");
        fullscreenToggle.getStyleClass().add("settings-checkbox");
        fullscreenToggle.setSelected(GameSettings.isFullscreenEnabled());
        fullscreenToggle.selectedProperty().addListener((obs, oldValue, selected) ->
                GameSettings.setFullscreenEnabled(selected));

        VBox body = new VBox(14, title, subtitle, musicToggle, soundToggle, fullscreenToggle);
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
        DialogFrameStyle.hideSystemFrame(dialog);
    }
}
