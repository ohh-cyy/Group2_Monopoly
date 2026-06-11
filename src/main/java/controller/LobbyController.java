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
 * 主大厅的 FXML 控制器（{@code lobby-view.fxml}）。
 * <p>
 * 提供三种入口：主持联机、加入远程服务器或开始本地热座对局。
 * 主持时管理内嵌 {@link GameServer} 的生命周期。
 */
public class LobbyController {
    /** 加入/主持时的服务器地址与端口输入框。 */
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    /** 加入时发送给服务器的显示名称。 */
    @FXML
    private TextField nameField;
    /** 连接、大厅与错误消息的状态行。 */
    @FXML
    private Label statusLabel;
    /** 已连接大厅玩家的实时列表（主持方可见开始按钮）。 */
    @FXML
    private ListView<String> playerList;
    @FXML
    private Button hostBtn;
    @FXML
    private Button joinBtn;
    /** 仅当玩家数量合法时对主持方启用。 */
    @FXML
    private Button startBtn;
    @FXML
    private Label achievementProgressLabel;
    /** 本地游戏的玩家数量选择器（2–5）。 */
    @FXML
    private ComboBox<Integer> localPlayerCount;

    /** 通过 {@link SettingsOverlay} 打开游戏叠加层所用的窗口。 */
    private Stage stage;
    /** 本客户端主持时的内嵌服务器实例。 */
    private GameServer gameServer;
    /** 大厅阶段的网络客户端（游戏开始时移交）。 */
    private NetworkClient client;
    /** 成功加入后分配的座位（此前为 -1）。 */
    private int localSeat = -1;
    /** 本客户端是否创建了房间并可开始对局。 */
    private boolean isHost;

    /**
     * 提供主舞台，以便以模态方式打开游戏视图。
     *
     * @param stage 所属窗口
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

    // 点击加入游戏按钮
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

    // 连接客户端
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
