package controller;

import javafx.application.Platform;
import model.achievement.AchievementManager;
import ui.AchievementUi;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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

/**
 * FXML controller for the main lobby ({@code lobby-view.fxml}).
 * <p>
 * Offers three entry paths: host an online game, join a remote server, or start a
 * local hot-seat match. Manages embedded {@link GameServer} lifecycle when hosting.
 */
public class LobbyController {
    /** Server address and port inputs for join/host. */
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    /** Display name sent to the server on join. */
    @FXML
    private TextField nameField;
    /** Status line for connection, lobby, and error messages. */
    @FXML
    private Label statusLabel;
    /** Live list of connected lobby players (host sees start button). */
    @FXML
    private ListView<String> playerList;
    @FXML
    private Button hostBtn;
    @FXML
    private Button joinBtn;
    /** Enabled only for the host when player count is valid. */
    @FXML
    private Button startBtn;
    @FXML
    private Label achievementProgressLabel;
    /** Player count selector for local games (2–5). */
    @FXML
    private ComboBox<Integer> localPlayerCount;

    /** Window used to open game overlays via {@link SettingsOverlay}. */
    private Stage stage;
    /** Embedded server instance when this client is hosting. */
    private GameServer gameServer;
    /** Lobby-phase network client (handed off when the game starts). */
    private NetworkClient client;
    /** Seat assigned after a successful join (-1 until then). */
    private int localSeat = -1;
    /** Whether this client created the room and may start the match. */
    private boolean isHost;

    /**
     * Supplies the primary stage so game views can be opened modally.
     *
     * @param stage owning window
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * 初始化大厅，设置端口和玩家人数
     */
    @FXML
    public void initialize() {
        hostField.setText("127.0.0.1");
        portField.setText("5947");
        localPlayerCount.getItems().setAll(2, 3, 4, 5);
        localPlayerCount.setValue(4);
        refreshAchievementProgress();
        Platform.runLater(() -> unlockAchievement(AchievementManager.WELCOME_LOBBY));
    }

    // 创建新主机
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

//点击加入游戏按钮
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
            Integer selectedCount = localPlayerCount.getValue();
            SettingsOverlay.openLocalGame(stage, selectedCount != null ? selectedCount : 4);
        } catch (IOException e) {
            statusLabel.setText("Unable to open local game: " + e.getMessage());
        }
    }

//连接客户端
    private void connectClient(String host, int port, String name, boolean hostFlag) throws IOException {
        client = new NetworkClient();
        client.connect(host, port, this::handleServerMessage);
        client.join(name, hostFlag);
    }

    //处理服务器消息
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

    //点击成就按钮
    @FXML
    private void onAchievementsClick() {
        AchievementUi.showLibraryDialog(statusLabel);
        refreshAchievementProgress();
    }

    //解锁名字成就
    private void unlockNameAchievementIfReady() {
        if (nameField != null && nameField.getText() != null && nameField.getText().trim().length() >= 2) {
            unlockAchievement(AchievementManager.SET_NAME);
        }
    }

    //解锁成就
    private void unlockAchievement(String achievementId) {
        AchievementUi.unlockAndShow(achievementId, statusLabel);
        refreshAchievementProgress();
    }

    //刷新成就进度
    private void refreshAchievementProgress() {
        if (achievementProgressLabel != null) {
            achievementProgressLabel.setText("Achievements " + AchievementManager.progressText());
        }
    }

    //更新大厅
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

    //打开网络游戏
    private void openNetworkGame(ServerMessage message) {
        try {
            NetworkClient gameClient = client;
            SettingsOverlay.openNetworkGame(stage, gameClient, localSeat, message.state, gameClient::close);
            client = null;
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            statusLabel.setText("Unable to open game: " + detail);
        }
    }

    //解析端口
    private int parsePort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return 8888;
        }
    }

    //停止服务器
    private void stopServer() {
        if (gameServer != null) {
            gameServer.close();
            gameServer = null;
        }
    }

    //关闭客户端
    private void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
