package controller;

import javafx.application.Platform;
import model.achievement.AchievementManager;
import ui.AchievementUi;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import network.client.NetworkClient;
import network.protocol.LobbyPlayerDto;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;
import network.server.GameServer;
import network.server.GameSession;
import ui.SettingsOverlay;

import java.io.IOException;

public class LobbyController {
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField nameField;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<String> playerList;
    @FXML
    private Button hostBtn;
    @FXML
    private Button joinBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Label achievementProgressLabel;

    private Stage stage;
    private GameServer gameServer;
    private NetworkClient client;
    private int localSeat = -1;
    private boolean isHost;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        hostField.setText("127.0.0.1");
        portField.setText("5947");
        refreshAchievementProgress();
        Platform.runLater(() -> unlockAchievement(AchievementManager.WELCOME_LOBBY));
    }

    @FXML
    private void onHostClick() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Please enter a name.");
            return;
        }
        try {
            unlockNameAchievementIfReady();
            unlockAchievement(AchievementManager.CHOOSE_MODE);
            int port = parsePort();
            stopServer();
            closeClient();
            gameServer = new GameServer(port);
            gameServer.start();
            connectClient("127.0.0.1", port, name, true);
            hostBtn.setDisable(true);
            joinBtn.setDisable(true);
            statusLabel.setText("Server running on port " + port
                    + ". Share your IP with other players.");
        } catch (Exception e) {
            statusLabel.setText("Failed to start server: " + e.getMessage());
        }
    }

    @FXML
    private void onJoinClick() {
        String name = nameField.getText().trim();
        String host = hostField.getText().trim();
        if (name.isEmpty() || host.isEmpty()) {
            statusLabel.setText("Please enter a name and server address.");
            return;
        }
        try {
            unlockNameAchievementIfReady();
            unlockAchievement(AchievementManager.CHOOSE_MODE);
            int port = parsePort();
            closeClient();
            connectClient(host, port, name, false);
            hostBtn.setDisable(true);
            joinBtn.setDisable(true);
            statusLabel.setText("Connecting to " + host + ":" + port + "...");
        } catch (Exception e) {
            statusLabel.setText("Join failed: " + e.getMessage());
        }
    }

    @FXML
    private void onStartClick() {
        if (!isHost || client == null) {
            return;
        }
        client.startGame();
        statusLabel.setText("Starting game...");
    }

    @FXML
    private void onLocalGameClick() {
        stopServer();
        closeClient();
        unlockNameAchievementIfReady();
        unlockAchievement(AchievementManager.CHOOSE_MODE);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/game-view.fxml"));
            loader.load();
            GameController controller = loader.getController();
            controller.startLocalGame();
            stage.setTitle("Monopoly Deal - Local");
            stage.setScene(SettingsOverlay.createGameScene(loader.getRoot(), stage));
        } catch (IOException e) {
            statusLabel.setText("Unable to open local game: " + e.getMessage());
        }
    }

    private void connectClient(String host, int port, String name, boolean hostFlag) throws IOException {
        client = new NetworkClient();
        client.connect(host, port, this::handleServerMessage);
        client.join(name, hostFlag);
    }

    private void handleServerMessage(ServerMessage message) {
        Platform.runLater(() -> {
            if (message == null || message.type == null) {
                return;
            }
            switch (message.type) {
                case MessageTypes.JOINED -> {
                    localSeat = message.seat;
                    isHost = message.youAreHost;
                    statusLabel.setText(message.text);
                    if (isHost) {
                        startBtn.setDisable(false);
                    }
                }
                case MessageTypes.LOBBY -> updateLobby(message);
                case MessageTypes.GAME_STARTED -> openNetworkGame(message);
                case MessageTypes.ERROR -> statusLabel.setText(message.text);
                default -> {
                }
            }
        });
    }

    @FXML
    private void onAchievementsClick() {
        AchievementUi.showLibraryDialog(statusLabel);
        refreshAchievementProgress();
    }

    private void unlockNameAchievementIfReady() {
        if (nameField != null && nameField.getText() != null && nameField.getText().trim().length() >= 2) {
            unlockAchievement(AchievementManager.SET_NAME);
        }
    }

    private void unlockAchievement(String achievementId) {
        AchievementUi.unlockAndShow(achievementId, statusLabel);
        refreshAchievementProgress();
    }

    private void refreshAchievementProgress() {
        if (achievementProgressLabel != null) {
            achievementProgressLabel.setText("Achievements " + AchievementManager.progressText());
        }
    }

    private void updateLobby(ServerMessage message) {
        playerList.getItems().clear();
        int count = 0;
        for (LobbyPlayerDto player : message.lobbyPlayers) {
            String tag = player.host ? " (Host)" : "";
            playerList.getItems().add((player.seat + 1) + ". " + player.name + tag);
            count++;
        }
        statusLabel.setText(message.text);
        if (isHost) {
            startBtn.setDisable(count < GameSession.MIN_PLAYERS || count > GameSession.MAX_PLAYERS);
        }
    }

    private void openNetworkGame(ServerMessage message) {
        var resource = getClass().getResource("/ui/network-game-view.fxml");
        if (resource == null) {
            statusLabel.setText("Unable to open game: missing network-game-view.fxml. Please run mvn compile first");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(resource);
            var root = loader.load();
            NetworkGameController controller = loader.getController();
            if (controller == null) {
                statusLabel.setText("Unable to open game: controller not loaded");
                return;
            }
            NetworkClient gameClient = client;
            stage.setTitle("Monopoly Deal - Online");
            stage.setScene(SettingsOverlay.createGameScene((javafx.scene.Parent) root, stage, gameClient::close));
            stage.show();
            controller.startOnlineGame(gameClient, localSeat, message.state);
            client = null;
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            statusLabel.setText("Unable to open game: " + detail);
        }
    }

    private int parsePort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return 8888;
        }
    }

    private void stopServer() {
        if (gameServer != null) {
            gameServer.close();
            gameServer = null;
        }
    }

    private void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
